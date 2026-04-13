package com.meng.lovespace.user.util;

import java.time.MonthDay;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 基于 {@link MonthDay} 计算下一次公历纪念日与倒计时。
 */
public final class MemorialCountdownCalculator {

    private MemorialCountdownCalculator() {}

    /**
     * 计算下一次该月日出现的本地日期（含今天若月日相同）。
     *
     * @param monthDay 任意年份的纪念日（取月、日）
     * @param zone 时区
     */
    public static LocalDate nextOccurrenceDate(MonthDay monthDay, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        LocalDate thisYear = monthDay.atYear(today.getYear());
        if (!thisYear.isBefore(today)) {
            return thisYear;
        }
        return monthDay.atYear(today.getYear() + 1);
    }

    /**
     * @return 从今天到下一次纪念日的整天数（同一天为 0）
     */
    public static long daysUntilNext(LocalDate memorialDate, ZoneId zone) {
        MonthDay md = MonthDay.from(memorialDate);
        LocalDate next = nextOccurrenceDate(md, zone);
        return ChronoUnit.DAYS.between(LocalDate.now(zone), next);
    }

    /**
     * 到「下一次纪念日」的毫秒数：非当天则到该日 00:00；当天则到当日 23:59:59.999。
     */
    public static long millisecondsUntilNext(LocalDate memorialDate, ZoneId zone) {
        MonthDay md = MonthDay.from(memorialDate);
        LocalDate today = LocalDate.now(zone);
        LocalDate next = nextOccurrenceDate(md, zone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        if (next.equals(today)) {
            ZonedDateTime endOfDay = today.atTime(LocalTime.MAX).atZone(zone);
            return ChronoUnit.MILLIS.between(now, endOfDay);
        }
        ZonedDateTime start = next.atStartOfDay(zone);
        return ChronoUnit.MILLIS.between(now, start);
    }

    public static boolean isToday(LocalDate memorialDate, ZoneId zone) {
        MonthDay md = MonthDay.from(memorialDate);
        LocalDate today = LocalDate.now(zone);
        return md.equals(MonthDay.from(today));
    }
}
