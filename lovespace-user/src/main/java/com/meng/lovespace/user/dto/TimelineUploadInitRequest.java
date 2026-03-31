package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 初始化时间轴分片上传。
 *
 * @param fileName 原始文件名（须含合法扩展名）
 * @param fileSize 文件总字节数
 * @param contentType 可选 MIME，供对象存储写入 Content-Type
 */
public record TimelineUploadInitRequest(
        @NotBlank(message = "fileName is required") String fileName,
        @Min(value = 1, message = "fileSize must be positive") long fileSize,
        String contentType) {}
