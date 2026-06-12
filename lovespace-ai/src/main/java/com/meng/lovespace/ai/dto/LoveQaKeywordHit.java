package com.meng.lovespace.ai.dto;

/**
 * MySQL 关键词检索单条命中（文档级），将转换为 {@link org.springframework.ai.document.Document} 参与 RRF。
 */
public record LoveQaKeywordHit(
        String documentId,
        String title,
        String category,
        String scope,
        String coupleId,
        String textPreview,
        double bm25Score,
        String ingestedAt) {}
