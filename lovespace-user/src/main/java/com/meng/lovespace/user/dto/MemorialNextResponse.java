package com.meng.lovespace.user.dto;

import java.time.LocalDate;

/**
 * 距离当前最近的下一个纪念日（含倒计时）。
 */
public record MemorialNextResponse(
        MemorialDayResponse memorial,
        LocalDate nextOccurrenceDate,
        long daysUntil,
        long millisecondsUntilNext,
        boolean today) {}
