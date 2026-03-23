package com.meng.lovespace.user.dto;

import java.time.LocalDateTime;

/**
 * 相册 API 视图。
 */
public record AlbumResponse(
        String id,
        String coupleId,
        String name,
        String coverImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
