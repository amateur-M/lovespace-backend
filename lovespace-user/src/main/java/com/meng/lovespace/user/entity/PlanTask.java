package com.meng.lovespace.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 计划子任务，映射表 {@code plan_tasks}。
 */
@Data
@TableName("plan_tasks")
public class PlanTask {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("plan_id")
    private String planId;

    @TableField("title")
    private String title;

    @TableField("assignee_id")
    private String assigneeId;

    /** 0 未完成 1 已完成 */
    @TableField("is_completed")
    private Integer isCompleted;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("due_date")
    private LocalDate dueDate;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
