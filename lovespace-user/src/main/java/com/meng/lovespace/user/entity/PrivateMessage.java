package com.meng.lovespace.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 私密消息实体，映射表 {@code private_messages}。
 */
@Data
@TableName("private_messages")
public class PrivateMessage {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("couple_id")
    private String coupleId;

    @TableField("sender_id")
    private String senderId;

    @TableField("receiver_id")
    private String receiverId;

    @TableField("content")
    private String content;

    @TableField("message_type")
    private String messageType;

    @TableField("is_scheduled")
    private Integer isScheduled;

    @TableField("scheduled_time")
    private LocalDateTime scheduledTime;

    @TableField("is_read")
    private Integer isRead;

    @TableField("read_time")
    private LocalDateTime readTime;

    @TableField("is_retracted")
    private Integer isRetracted;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
