package com.meng.lovespace.user.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 恋爱时间轴媒体上传校验，前缀 {@code lovespace.timeline-upload}。
 *
 * <p>图片与视频分档大小上限；扩展名均为小写、不含点。分片合并后写入与直传相同的 {@code timeline/} 路径。
 *
 * @param imageMaxSizeBytes 单张图片最大字节数
 * @param videoMaxSizeBytes 单个视频最大字节数
 * @param imageExtensions 允许的图片扩展名
 * @param videoExtensions 允许的视频扩展名
 * @param chunkSizeBytes 分片上传每片大小（除末片外）
 * @param pendingSessionHours 未完成分片会话在磁盘上的保留时间，超时由定时任务清理
 */
@ConfigurationProperties(prefix = "lovespace.timeline-upload")
public record TimelineUploadProperties(
        long imageMaxSizeBytes,
        long videoMaxSizeBytes,
        List<String> imageExtensions,
        List<String> videoExtensions,
        long chunkSizeBytes,
        long pendingSessionHours) {}
