package com.meng.lovespace.user.dto;

/** 分片会话创建结果。 */
public record MediaChunkInitResponse(String uploadId, long chunkSize, int totalChunks) {}
