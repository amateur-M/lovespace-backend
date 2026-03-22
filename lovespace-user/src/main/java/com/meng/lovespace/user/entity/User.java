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
 * 用户实体，映射表 {@code users}。
 *
 * <p>{@code created_at}/{@code updated_at} 由 MyBatis-Plus 元数据自动填充。
 */
@Data
@TableName("users")
public class User {

    /** 主键，UUID 字符串 */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 登录名，唯一 */
    @TableField("username")
    private String username;

    /** 邮箱，唯一 */
    @TableField("email")
    private String email;

    /** BCrypt 等算法哈希后的密码 */
    @TableField("password_hash")
    private String passwordHash;

    /** 头像访问 URL */
    @TableField("avatar_url")
    private String avatarUrl;

    /** 性别（业务约定枚举数值） */
    @TableField("gender")
    private Integer gender;

    /** 生日 */
    @TableField("birthday")
    private LocalDate birthday;

    /** 账号状态，如 1 正常 0 禁用 */
    @TableField("status")
    private Integer status;

    /** 创建时间（插入时自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（插入/更新时自动填充） */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

