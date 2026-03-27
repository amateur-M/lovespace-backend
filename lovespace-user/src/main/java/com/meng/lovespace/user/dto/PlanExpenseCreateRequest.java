package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 创建计划消费记录。 */
public record PlanExpenseCreateRequest(
        @NotBlank(message = "expenseType is required") String expenseType,
        @NotNull(message = "amount is required")
                @DecimalMin(value = "0.01", message = "amount must be positive")
                BigDecimal amount,
        LocalDate spentDate,
        String note) {}
