package com.meng.lovespace.user.dto;

import java.time.LocalDateTime;

/** 恋爱问答单条历史消息。 */
public record LoveQaMessageLine(long id, String role, String content, LocalDateTime createdAt) {}
