package com.meng.lovespace.user.dto;

import java.util.List;

/** 恋爱知识库文档分页列表。 */
public record LoveQaDocumentPageResponse(
        long total, long page, long pageSize, List<LoveQaDocumentSummary> items) {}
