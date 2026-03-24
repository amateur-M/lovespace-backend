package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

/**
 * 创建定时私密消息请求。
 */
public record ScheduledMessageCreateRequest(
        @NotNull(message = "scheduledTime is required") @Future(message = "scheduledTime must be in the future")
                LocalDateTime scheduledTime,
        @NotBlank(message = "coupleId is required") String coupleId,
        @NotBlank(message = "receiverId is required") String receiverId,
        @NotBlank(message = "content is required") String content,
        @NotBlank(message = "messageType is required")
                @Pattern(regexp = "text|image|voice|letter", message = "invalid messageType")
                String messageType) {}
