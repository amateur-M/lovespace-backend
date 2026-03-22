package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 更新个人资料请求；字段为 {@code null} 表示不修改该列。
 *
 * @param avatarUrl 头像地址
 * @param gender 性别（业务约定数值）
 * @param birthday 生日
 */
public record UpdateProfileRequest(
        @Size(max = 500) String avatarUrl, Integer gender, LocalDate birthday) {}

