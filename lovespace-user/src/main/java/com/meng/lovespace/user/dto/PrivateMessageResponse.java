package com.meng.lovespace.user.dto;

import java.time.LocalDateTime;

/**
 * 私密消息 API 视图。
 */
public record PrivateMessageResponse(
        String id,
        String coupleId,
        String senderId,
        String receiverId,
        String content,
        String messageType,
        Integer isScheduled,
        LocalDateTime scheduledTime,
        Integer isRead,
        LocalDateTime readTime,
        Integer isRetracted,
        LocalDateTime createdAt) {}
