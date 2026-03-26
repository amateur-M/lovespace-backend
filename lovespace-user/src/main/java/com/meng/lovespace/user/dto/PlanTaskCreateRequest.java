package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * 在计划下创建子任务请求。
 */
public record PlanTaskCreateRequest(
        @NotBlank(message = "title is required") String title, String assigneeId, LocalDate dueDate) {}
