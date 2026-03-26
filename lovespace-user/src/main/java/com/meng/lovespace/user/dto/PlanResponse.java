package com.meng.lovespace.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 共同计划响应（含子任务列表）。
 */
public record PlanResponse(
        String id,
        String coupleId,
        String title,
        String description,
        String planType,
        Integer priority,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer progress,
        BigDecimal budgetTotal,
        BigDecimal budgetSpent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PlanTaskResponse> tasks) {}
