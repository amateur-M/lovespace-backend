package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.LoveQaConversationPageResponse;
import com.meng.lovespace.user.dto.LoveQaMessagesResponse;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.LoveQaConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 恋爱问答历史：会话列表与消息明细（依赖 MySQL 表 love_qa_*）。
 */
@Tag(name = "AI", description = "恋爱知识库历史")
@Validated
@RestController
@RequestMapping("/api/v1/ai/love-qa")
@RequiredArgsConstructor
public class LoveQAHistoryController {

    private final LoveQaConversationService loveQaConversationService;

    @Operation(summary = "我的恋爱问答会话列表")
    @GetMapping("/conversations")
    public ApiResponse<LoveQaConversationPageResponse> listConversations(
            Authentication auth,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) long page,
            @RequestParam(value = "pageSize", defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(loveQaConversationService.pageConversations(p.userId(), page, pageSize));
    }

    @Operation(summary = "某会话下的全部消息")
    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<LoveQaMessagesResponse> listMessages(
            Authentication auth, @PathVariable("conversationId") String conversationId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(loveQaConversationService.listMessages(p.userId(), conversationId));
    }
}
