package com.meng.lovespace.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新共同计划请求；字段为 {@code null} 表示不修改。
 */
public record PlanUpdateRequest(
        String title,
        String description,
        String planType,
        Integer priority,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer progress,
        BigDecimal budgetTotal,
        BigDecimal budgetSpent) {}
