package com.meng.lovespace.user.dto;

/** 知识库文档入库结果（含台账 documentId 与分片数）。 */
public record LoveQaIngestResponseData(String documentId, String status, int chunkCount) {}
