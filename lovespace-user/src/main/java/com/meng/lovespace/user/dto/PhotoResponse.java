package com.meng.lovespace.user.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 照片 API 视图。
 */
public record PhotoResponse(
        String id,
        String albumId,
        String uploaderId,
        String imageUrl,
        String thumbnailUrl,
        String description,
        String locationJson,
        LocalDate takenDate,
        String tagsJson,
        Integer isFavorite,
        LocalDateTime createdAt) {}
