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
 * 纪念日，映射表 {@code memorial_days}。
 *
 * <p>每年重复的日期由 {@link #memorialDate} 的月、日决定；年份仅作展示或首次纪念年份参考。
 */
@Data
@TableName("memorial_days")
public class MemorialDay {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("couple_id")
    private String coupleId;

    @TableField("user_id")
    private String userId;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("memorial_date")
    private LocalDate memorialDate;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
