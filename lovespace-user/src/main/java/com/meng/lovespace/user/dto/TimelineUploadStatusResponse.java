package com.meng.lovespace.user.dto;

import java.util.List;

/**
 * 分片上传进度（用于断点续传：仅上传缺失分片）。
 *
 * @param uploadId 会话 ID
 * @param chunkSize 单片大小
 * @param totalChunks 总片数
 * @param totalSize 文件总大小
 * @param uploadedIndices 已写入的分片下标（升序）
 * @param complete 是否已全部就绪可调用 complete
 */
public record TimelineUploadStatusResponse(
        String uploadId,
        long chunkSize,
        int totalChunks,
        long totalSize,
        List<Integer> uploadedIndices,
        boolean complete) {}
