package com.meng.lovespace.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 共同计划，映射表 {@code couple_plans}。
 *
 * <p>{@code coupleId} 对应 {@code couple_binding.id}。
 */
@Data
@TableName("couple_plans")
public class Plan {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("couple_id")
    private String coupleId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    /** goal / travel / event */
    @TableField("plan_type")
    private String planType;

    @TableField("priority")
    private Integer priority;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    @TableField("status")
    private String status;

    /** 0–100 */
    @TableField("progress")
    private Integer progress;

    @TableField("budget_total")
    private BigDecimal budgetTotal;

    @TableField("budget_spent")
    private BigDecimal budgetSpent;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
