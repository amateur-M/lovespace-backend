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
 * 恋爱时间轴记录，映射表 {@code love_records}。
 */
@Data
@TableName("love_records")
public class LoveRecord {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("couple_id")
    private String coupleId;

    @TableField("author_id")
    private String authorId;

    @TableField("record_date")
    private LocalDate recordDate;

    @TableField("content")
    private String content;

    @TableField("mood")
    private String mood;

    /** 位置信息 JSON 字符串 */
    @TableField("location_json")
    private String locationJson;

    @TableField("visibility")
    private Integer visibility;

    /** 标签等 JSON 字符串 */
    @TableField("tags_json")
    private String tagsJson;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
