package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建共同计划请求。
 */
public record PlanCreateRequest(
        @NotBlank(message = "coupleId is required") String coupleId,
        @NotBlank(message = "title is required") String title,
        String description,
        @NotBlank(message = "planType is required") String planType,
        Integer priority,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer progress,
        BigDecimal budgetTotal) {}
