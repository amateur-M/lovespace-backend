package com.meng.lovespace.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 通用对话请求体。
 *
 * @param message 用户消息
 */
public record AiChatRequest(@NotBlank(message = "message 不能为空") String message) {}
