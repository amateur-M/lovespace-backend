package com.meng.lovespace.user.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 计划子任务响应。
 */
public record PlanTaskResponse(
        String id,
        String planId,
        String title,
        String assigneeId,
        boolean completed,
        LocalDateTime completedAt,
        LocalDate dueDate,
        LocalDateTime createdAt) {}
