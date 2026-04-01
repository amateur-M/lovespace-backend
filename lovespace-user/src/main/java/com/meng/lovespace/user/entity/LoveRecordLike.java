package com.meng.lovespace.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 恋爱记录点赞，映射表 {@code love_record_likes}。 */
@Data
@TableName("love_record_likes")
public class LoveRecordLike {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("record_id")
    private String recordId;

    @TableField("user_id")
    private String userId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
