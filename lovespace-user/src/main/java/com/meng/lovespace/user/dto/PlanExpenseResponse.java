package com.meng.lovespace.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 单条计划消费记录。 */
public record PlanExpenseResponse(
        String id,
        String planId,
        String expenseType,
        BigDecimal amount,
        LocalDate spentDate,
        String note,
        String createdBy,
        LocalDateTime createdAt) {}
