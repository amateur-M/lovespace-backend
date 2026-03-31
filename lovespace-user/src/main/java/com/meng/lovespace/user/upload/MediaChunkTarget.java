package com.meng.lovespace.user.upload;

/**
 * 分片合并后的存储目标：时间轴媒体或相册照片。
 */
public enum MediaChunkTarget {
    TIMELINE,
    ALBUM;

    /** @param raw 不区分大小写，如 {@code timeline}、{@code ALBUM} */
    public static MediaChunkTarget fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("target is required");
        }
        return MediaChunkTarget.valueOf(raw.trim().toUpperCase());
    }
}
