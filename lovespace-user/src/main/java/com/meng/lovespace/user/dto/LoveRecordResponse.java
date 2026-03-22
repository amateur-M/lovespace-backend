package com.meng.lovespace.user.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 恋爱记录 API 视图。
 */
public record LoveRecordResponse(
        String id,
        String coupleId,
        String authorId,
        LocalDate recordDate,
        String content,
        String mood,
        String locationJson,
        Integer visibility,
        String tagsJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
