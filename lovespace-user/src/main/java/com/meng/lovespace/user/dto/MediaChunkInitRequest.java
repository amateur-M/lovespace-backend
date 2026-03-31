package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 初始化公共分片上传。
 *
 * @param target {@code TIMELINE} 或 {@code ALBUM}
 * @param albumId 目标为相册时必填
 * @param fileName 原始文件名
 * @param fileSize 总字节数
 * @param contentType 可选 MIME
 */
public record MediaChunkInitRequest(
        @NotBlank(message = "target is required") String target,
        String albumId,
        @NotBlank(message = "fileName is required") String fileName,
        @Min(value = 1, message = "fileSize must be positive") long fileSize,
        String contentType) {}
