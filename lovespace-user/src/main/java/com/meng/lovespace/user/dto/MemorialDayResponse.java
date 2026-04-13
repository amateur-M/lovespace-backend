package com.meng.lovespace.user.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 纪念日详情响应。
 */
public record MemorialDayResponse(
        String id,
        String coupleId,
        String userId,
        String name,
        String description,
        LocalDate memorialDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
