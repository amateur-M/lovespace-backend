package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 更新个人资料；字段为 {@code null} 表示不修改该列。
 *
 * @param avatarUrl 头像地址
 * @param gender 性别（业务约定数值）
 * @param birthday 生日
 * @param username 用户名（展示名）；非空且与当前不同时校验唯一
 * @param email 邮箱；非 {@code null} 时写入：空串表示清空；非空时校验格式与唯一
 */
public record UpdateProfileRequest(
        @Size(max = 500) String avatarUrl,
        Integer gender,
        LocalDate birthday,
        @Size(max = 50) String username,
        String email) {}
