package com.meng.lovespace.user.dto;

/**
 * 分片上传会话创建结果。
 *
 * @param uploadId 会话 ID（UUID）
 * @param chunkSize 单片字节数（除末片外均为此大小）
 * @param totalChunks 片数
 */
public record TimelineUploadInitResponse(String uploadId, long chunkSize, int totalChunks) {}
