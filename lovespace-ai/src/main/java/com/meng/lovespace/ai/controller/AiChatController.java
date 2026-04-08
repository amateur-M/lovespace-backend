package com.meng.lovespace.ai.controller;

import com.meng.lovespace.ai.dto.AiChatRequest;
import com.meng.lovespace.ai.dto.AiChatResponseData;
import com.meng.lovespace.ai.service.AiChatService;
import com.meng.lovespace.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 通用对话接口。
 */
@Tag(name = "AI", description = "大模型对话")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * 单轮文本对话。
     *
     * @param request 用户消息
     * @return 统一响应，data 为模型回复
     */
    @Operation(summary = "通用对话")
    @PostMapping("/chat")
    public ApiResponse<AiChatResponseData> chat(@Valid @RequestBody AiChatRequest request) {
        String reply = aiChatService.chat(request.message());
        return ApiResponse.ok(new AiChatResponseData(reply));
    }
}
