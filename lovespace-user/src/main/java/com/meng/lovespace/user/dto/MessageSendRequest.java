package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 发送私密消息请求。
 */
public record MessageSendRequest(
        @NotBlank(message = "coupleId is required") String coupleId,
        @NotBlank(message = "receiverId is required") String receiverId,
        @NotBlank(message = "content is required") String content,
        @NotBlank(message = "messageType is required")
        @Pattern(regexp = "text|image|voice|letter", message = "invalid messageType")
        String messageType) {}
