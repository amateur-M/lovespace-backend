package com.meng.lovespace.user.controller;

import com.meng.lovespace.ai.api.LoveQaChatFacade;
import com.meng.lovespace.ai.dto.LoveQaChatParams;
import com.meng.lovespace.ai.dto.LoveQaChatRequest;
import com.meng.lovespace.ai.dto.LoveQaChatResponseData;
import com.meng.lovespace.ai.dto.LoveQaChatResult;
import com.meng.lovespace.ai.dto.LoveQaIngestRequest;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.LoveQaConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 恋爱知识库 RAG：入库与多轮问答（需 Milvus + EmbeddingModel + Redis）。
 */
@Slf4j
@Tag(name = "AI", description = "恋爱知识库 RAG")
@RestController
@RequestMapping("/api/v1/ai/love-qa")
@RequiredArgsConstructor
@ConditionalOnBean(LoveQaChatFacade.class)
public class LoveQAController {

    private final LoveQaChatFacade loveQaChatFacade;
    private final LoveQaConversationService loveQaConversationService;

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
}
