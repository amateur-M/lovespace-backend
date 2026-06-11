package com.meng.lovespace.user.dto;

import java.time.LocalDateTime;

/** 恋爱知识库文档列表项。 */
public record LoveQaDocumentSummary(
        String documentId,
        String coupleId,
        String title,
        String sourceUrl,
        String category,
        String scope,
        String status,
        int chunkCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
