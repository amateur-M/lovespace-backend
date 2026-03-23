package com.meng.lovespace.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 情侣相册，映射表 {@code couple_albums}。
 *
 * <p>{@code coupleId} 对应 {@code couple_binding.id}。
 */
@Data
@TableName("couple_albums")
public class Album {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 情侣绑定主键 */
    @TableField("couple_id")
    private String coupleId;

    @TableField("name")
    private String name;

    @TableField("cover_image_url")
    private String coverImageUrl;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
