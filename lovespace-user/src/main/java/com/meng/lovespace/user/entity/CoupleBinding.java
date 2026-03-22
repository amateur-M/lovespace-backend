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
 * 情侣绑定实体，映射表 {@code couple_binding}。
 *
 * <p>邀请待接受时 {@link com.meng.lovespace.user.couple.CoupleBindingStatus#PENDING}，
 * {@code user_id1} 为邀请方，{@code user_id2} 为被邀请方；成为情侣后两用户 ID 按字典序存储。
 */
@Data
@TableName("couple_binding")
public class CoupleBinding {

    /** 主键 */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户 1 */
    @TableField("user_id1")
    private String userId1;

    /** 用户 2 */
    @TableField("user_id2")
    private String userId2;

    /** 恋爱开始日期 */
    @TableField("start_date")
    private LocalDate startDate;

    /** 恋爱天数（由开始日计算并持久化） */
    @TableField("relationship_days")
    private Integer relationshipDays;

    /** 状态：0 待接受 1 交往 2 冻结 3 解除 */
    @TableField("status")
    private Integer status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
