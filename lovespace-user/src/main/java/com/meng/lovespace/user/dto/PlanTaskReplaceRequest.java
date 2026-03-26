package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 更新计划子任务（整表替换语义）：标题、负责人、截止日、完成状态。
 */
public record PlanTaskReplaceRequest(
        @NotBlank @Size(max = 200) String title,
        String assigneeId,
        LocalDate dueDate,
        boolean completed) {}
