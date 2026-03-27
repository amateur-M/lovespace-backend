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
 * 计划消费记录，映射表 {@code plan_expenses}。
 */
@Data
@TableName("plan_expenses")
public class PlanExpense {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("plan_id")
    private String planId;

    @TableField("expense_type")
    private String expenseType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("spent_date")
    private LocalDate spentDate;

    @TableField("note")
    private String note;

    @TableField("created_by")
    private String createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
