package com.meng.lovespace.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meng.lovespace.ai.api.LoveQaChatFacade;
import com.meng.lovespace.ai.api.LoveQaStreamCallback;
import com.meng.lovespace.ai.dto.LoveQaChatParams;
import com.meng.lovespace.ai.dto.LoveQaChatRequest;
import com.meng.lovespace.ai.dto.LoveQaChatResponseData;
import com.meng.lovespace.ai.dto.LoveQaChatResult;
import com.meng.lovespace.ai.dto.LoveQaIngestRequest;
import com.meng.lovespace.ai.exception.LoveQaConversationAccessException;
import com.meng.lovespace.ai.exception.LoveQaConversationNotFoundException;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.LoveQaConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 恋爱知识库 RAG：入库与多轮问答（需 Milvus + EmbeddingModel + Redis）。
 */
@Slf4j
@Tag(name = "AI", description = "恋爱知识库 RAG")
@RestController
@RequestMapping("/api/v1/ai/love-qa")
public class LoveQAController {

    private final LoveQaChatFacade loveQaChatFacade;
    private final LoveQaConversationService loveQaConversationService;
    private final ObjectMapper objectMapper;

    public LoveQAController(
            LoveQaChatFacade loveQaChatFacade,
            LoveQaConversationService loveQaConversationService,
            ObjectMapper objectMapper) {
        this.loveQaChatFacade = loveQaChatFacade;
        this.loveQaConversationService = loveQaConversationService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "知识库文档入库")
    @PostMapping("/ingest")
    public ApiResponse<Void> ingest(Authentication auth, @Valid @RequestBody LoveQaIngestRequest request) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        Map<String, Object> meta = new HashMap<>();
        meta.put("ownerUserId", p.userId());
        if (request.title() != null && !request.title().isBlank()) {
            meta.put("title", request.title());
        }
        if (request.metadata() != null) {
            meta.putAll(request.metadata());
        }
        loveQaChatFacade.ingestDocument(request.text(), meta);
        return ApiResponse.ok();
    }

    @Operation(summary = "基于知识库的恋爱问答（多轮记忆）")
    @PostMapping("/chat")
    public ApiResponse<LoveQaChatResponseData> chat(Authentication auth, @Valid @RequestBody LoveQaChatRequest request) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        if (StringUtils.hasText(request.conversationId())) {
            loveQaConversationService.restoreRedisSessionIfMissing(
                    p.userId(), request.coupleId(), request.conversationId().trim());
        }
        LoveQaChatParams params =
                new LoveQaChatParams(
                        p.userId(), request.coupleId(), request.conversationId(), request.message());
        LoveQaChatResult result = loveQaChatFacade.chat(params);
        try {
            loveQaConversationService.appendChatRound(
                    result.conversationId(),
                    p.userId(),
                    request.coupleId(),
                    request.message(),
                    result.reply());
        } catch (Exception e) {
            log.error(
                    "love-qa persist to DB failed conversationId={}",
                    result.conversationId(),
                    e);
        }
        return ApiResponse.ok(new LoveQaChatResponseData(result.reply(), result.conversationId()));
    }

    /**
     * 流式问答：SSE，事件名 {@code meta} / {@code delta} / {@code done} / {@code error}，载荷为 JSON 字符串。
     */
    @Operation(summary = "基于知识库的恋爱问答（流式 SSE）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(Authentication auth, @Valid @RequestBody LoveQaChatRequest request) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        if (StringUtils.hasText(request.conversationId())) {
            loveQaConversationService.restoreRedisSessionIfMissing(
                    p.userId(), request.coupleId(), request.conversationId().trim());
        }
        LoveQaChatParams params =
                new LoveQaChatParams(
                        p.userId(), request.coupleId(), request.conversationId(), request.message());
        SseEmitter emitter = new SseEmitter(120_000L);
        AtomicReference<String> streamConversationId = new AtomicReference<>();
        Thread.ofVirtual()
                .start(
                        () -> {
                            try {
                                loveQaChatFacade.chatStream(
                                        params,
                                        new LoveQaStreamCallback() {
                                            @Override
                                            public void onMeta(String conversationId) {
                                                streamConversationId.set(conversationId);
                                                sendSse(
                                                        emitter,
                                                        "meta",
                                                        Map.of("conversationId", conversationId));
                                            }

                                            @Override
                                            public void onDelta(String text) {
                                                sendSse(emitter, "delta", Map.of("t", text));
                                            }

                                            @Override
                                            public void onCompleted(String fullReply) {
                                                String cid = streamConversationId.get();
                                                try {
                                                    loveQaConversationService.appendChatRound(
                                                            cid,
                                                            p.userId(),
                                                            request.coupleId(),
                                                            request.message(),
                                                            fullReply);
                                                } catch (Exception ex) {
                                                    log.error(
                                                            "love-qa stream persist to DB failed conversationId={}",
                                                            cid,
                                                            ex);
                                                }
                                                sendSse(
                                                        emitter,
                                                        "done",
                                                        Map.of(
                                                                "reply",
                                                                fullReply,
                                                                "conversationId",
                                                                cid != null ? cid : ""));
                                                emitter.complete();
                                            }
                                        });
                            } catch (LoveQaConversationNotFoundException e) {
                                sendSse(
                                        emitter,
                                        "error",
                                        Map.of("code", 40491, "message", "会话不存在或已过期"));
                                emitter.complete();
                            } catch (LoveQaConversationAccessException e) {
                                sendSse(
                                        emitter,
                                        "error",
                                        Map.of("code", 40391, "message", "无权访问该会话"));
                                emitter.complete();
                            } catch (Exception e) {
                                log.error("love-qa stream failed", e);
                                String msg = e.getMessage() != null ? e.getMessage() : "流式调用失败";
                                sendSse(emitter, "error", Map.of("code", 500, "message", msg));
                                emitter.complete();
                            }
                        });
        return emitter;
    }

    private void sendSse(SseEmitter emitter, String eventName, Map<String, ?> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            emitter.send(SseEmitter.event().name(eventName).data(json));
        } catch (IOException e) {
            log.debug("love-qa sse client disconnected: {}", e.getMessage());
            emitter.completeWithError(e);
        }
    }
}
