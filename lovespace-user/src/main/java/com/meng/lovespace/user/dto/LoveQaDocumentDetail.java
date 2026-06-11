package com.meng.lovespace.user.dto;

import java.time.LocalDateTime;

/** 恋爱知识库文档详情。 */
public record LoveQaDocumentDetail(
        String documentId,
        String coupleId,
        String ownerUserId,
        String title,
        String sourceUrl,
        String category,
        String scope,
        String status,
        int chunkCount,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
