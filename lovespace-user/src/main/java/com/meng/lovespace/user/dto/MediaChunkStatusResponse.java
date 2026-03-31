package com.meng.lovespace.user.dto;

import java.util.List;

/** 分片上传进度。 */
public record MediaChunkStatusResponse(
        String uploadId,
        long chunkSize,
        int totalChunks,
        long totalSize,
        List<Integer> uploadedIndices,
        boolean complete) {}
