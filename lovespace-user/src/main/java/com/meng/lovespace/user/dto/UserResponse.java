package com.meng.lovespace.user.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 返回给前端的用户视图（不含密码哈希）。
 *
 * @param id 用户 ID
 * @param username 用户名
 * @param email 邮箱
 * @param avatarUrl 头像 URL
 * @param gender 性别
 * @param birthday 生日
 * @param status 状态
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record UserResponse(
        String id,
        String username,
        String email,
        String avatarUrl,
        Integer gender,
        LocalDate birthday,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}

