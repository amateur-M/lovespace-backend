package com.meng.lovespace.user.dto;

import java.util.List;

/** 恋爱问答会话分页。 */
public record LoveQaConversationPageResponse(
        long total, long page, long pageSize, List<LoveQaConversationSummary> items) {}
