package com.meng.lovespace.user.dto;

import java.time.LocalDate;

/**
 * 未来窗口内的纪念日条目（用于列表与 Redis 缓存序列化）。
 */
public record MemorialUpcomingItemResponse(
        String id,
        String name,
        LocalDate memorialDate,
        LocalDate nextOccurrenceDate,
        long daysUntil,
        long millisecondsUntilNext,
        boolean today) {}
