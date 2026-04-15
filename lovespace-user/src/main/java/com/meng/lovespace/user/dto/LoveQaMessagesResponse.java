package com.meng.lovespace.user.dto;

import java.util.List;

/** 某会话下全部消息（按时间升序）。 */
public record LoveQaMessagesResponse(String conversationId, List<LoveQaMessageLine> messages) {}
