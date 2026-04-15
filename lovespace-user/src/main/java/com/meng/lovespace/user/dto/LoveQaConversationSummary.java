package com.meng.lovespace.user.dto;

import java.time.LocalDateTime;

/** 恋爱问答会话列表项。 */
public record LoveQaConversationSummary(String conversationId, String title, LocalDateTime updatedAt) {}
