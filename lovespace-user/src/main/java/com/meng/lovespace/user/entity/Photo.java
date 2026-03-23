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
 * 相册照片，映射表 {@code album_photos}。
 */
@Data
@TableName("album_photos")
public class Photo {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("album_id")
    private String albumId;

    @TableField("uploader_id")
    private String uploaderId;

    @TableField("image_url")
    private String imageUrl;

    @TableField("thumbnail_url")
    private String thumbnailUrl;

    @TableField("description")
    private String description;

    @TableField("location_json")
    private String locationJson;

    @TableField("taken_date")
    private LocalDate takenDate;

    @TableField("tags_json")
    private String tagsJson;

    @TableField("is_favorite")
    private Integer isFavorite;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
