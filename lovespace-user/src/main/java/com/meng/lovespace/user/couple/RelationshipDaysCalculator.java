package com.meng.lovespace.user.couple;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 根据恋爱开始日计算「在一起第几天」。
 *
 * <p>规则：包含开始日当天为第 1 天；{@code startDate} 晚于今天时返回 0。
 */
public final class RelationshipDaysCalculator {

    private RelationshipDaysCalculator() {}

    /**
     * 计算从 {@code startDate} 到 {@code today}（含首尾）的天数。
     *
     * @param startDate 恋爱开始日，可为 null（按 0 天处理）
     * @param today 当前日（一般传 {@link LocalDate#now(java.time.ZoneId)} 的业务时区日期）
     * @return 天数，至少为 0
     */
    public static int computeDays(LocalDate startDate, LocalDate today) {
        if (startDate == null || today == null) {
            return 0;
        }
        if (startDate.isAfter(today)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(startDate, today) + 1;
    }
}
