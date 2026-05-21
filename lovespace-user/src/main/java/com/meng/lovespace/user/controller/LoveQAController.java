package com.meng.lovespace.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meng.lovespace.ai.api.LoveQaChatFacade;
import com.meng.lovespace.ai.api.LoveQaStreamCallback;
import com.meng.lovespace.ai.dto.LoveQaChatParams;
import com.meng.lovespace.ai.dto.LoveQaChatRequest;
import com.meng.lovespace.ai.dto.LoveQaChatResponseData;
import com.meng.lovespace.ai.dto.LoveQaChatResult;
import com.meng.lovespace.ai.dto.LoveQaIngestRequest;
import com.meng.lovespace.ai.dto.RetrievedChunk;
import com.meng.lovespace.ai.exception.LoveQaConversationAccessException;
import com.meng.lovespace.ai.exception.LoveQaConversationNotFoundException;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.LoveQaConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
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

    @Operation(summary = "知识库文档入库（纯文本）")
    @PostMapping("/ingest")
    public ApiResponse<Void> ingest(Authentication auth, @Valid @RequestBody LoveQaIngestRequest request) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        Map<String, Object> meta = buildMetadata(p, request.title(), request.sourceUrl(), request.category(), request.coupleId(), request.metadata());
        loveQaChatFacade.ingestDocument(request.text(), meta);
        return ApiResponse.ok();
    }

    @Operation(summary = "知识库文档上传（支持文件 Multipart + 可选 URL 抓取）")
    @PostMapping(value = "/ingest/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> ingestFile(
            Authentication auth,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "sourceUrl", required = false) String sourceUrl,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "coupleId", required = false) String coupleId) throws IOException {

        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        String text;
        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to read file as UTF-8 text, filename={}", file.getOriginalFilename());
            throw new IllegalArgumentException("文件读取失败，仅支持 UTF-8 文本文件（.txt, .md 等）");
        }

        Map<String, Object> meta = buildMetadata(p, title, sourceUrl, category, coupleId, null);
        meta.put("originalFilename", file.getOriginalFilename());
        meta.put("fileSize", file.getSize());

        loveQaChatFacade.ingestDocument(text, meta);
        return ApiResponse.ok();
    }

    @Operation(summary = "知识库文档从 URL 入库（抓取网页文本）")
    @PostMapping("/ingest/url")
    public ApiResponse<Void> ingestFromUrl(Authentication auth, @Valid @RequestBody LoveQaIngestRequest request) throws Exception {
        if (!StringUtils.hasText(request.sourceUrl())) {
            throw new IllegalArgumentException("sourceUrl 不能为空");
        }
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();

        String text = fetchTextFromUrl(request.sourceUrl());
        Map<String, Object> meta = buildMetadata(p, request.title(), request.sourceUrl(), request.category(), request.coupleId(), request.metadata());

        loveQaChatFacade.ingestDocument(text, meta);
        return ApiResponse.ok();
    }

    private String fetchTextFromUrl(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "LoveSpace-RAG/1.0")
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("URL fetch failed: HTTP " + resp.statusCode());
        }
        // 简单提取：去掉 script/style 等标签的纯文本（可后续增强 Jsoup）
        String body = resp.body();
        // 极简清理，实际生产建议用 Jsoup 或 readability
        return body.replaceAll("<script[^>]*>.*?</script>", "")
                   .replaceAll("<style[^>]*>.*?</style>", "")
                   .replaceAll("<[^>]+>", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    private Map<String, Object> buildMetadata(JwtUserPrincipal p, String title, String sourceUrl, String category, String coupleId, Map<String, Object> extra) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("ownerUserId", p.userId());
        if (StringUtils.hasText(title)) {
            meta.put("title", title);
        }
        if (StringUtils.hasText(sourceUrl)) {
            meta.put("sourceUrl", sourceUrl);
        }
        if (StringUtils.hasText(category)) {
            meta.put("category", category);
        }
        if (StringUtils.hasText(coupleId)) {
            meta.put("coupleId", coupleId);
        }
        if (extra != null) {
            meta.putAll(extra);
        }
        return meta;
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
                                            public void onRetrieved(List<RetrievedChunk> chunks) {
                                                // 发送检索结果供前端可视化引用来源
                                                sendSse(emitter, "retrieved", Map.of("chunks", chunks));
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
