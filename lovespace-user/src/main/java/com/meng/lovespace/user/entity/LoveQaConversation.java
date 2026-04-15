package com.meng.lovespace.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 恋爱问答会话，表 {@code love_qa_conversations}。 */
@Data
@TableName("love_qa_conversations")
public class LoveQaConversation {

    @TableId(value = "conversation_id", type = IdType.INPUT)
    private String conversationId;

    @TableField("user_id")
    private String userId;

    @TableField("couple_id")
    private String coupleId;

    @TableField("title")
    private String title;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
