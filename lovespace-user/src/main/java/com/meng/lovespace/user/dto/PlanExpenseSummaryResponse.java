package com.meng.lovespace.user.dto;

import java.math.BigDecimal;

/**
 * 某计划下按消费类型汇总金额（与 {@link PlanResponse#budgetSpent()} 一致：total 为全部类型之和）。
 */
public record PlanExpenseSummaryResponse(
        BigDecimal lodging,
        BigDecimal transport,
        BigDecimal dining,
        BigDecimal other,
        BigDecimal total) {}
