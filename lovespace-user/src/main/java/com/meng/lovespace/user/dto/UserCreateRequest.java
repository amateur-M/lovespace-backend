package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理端/种子创建用户请求（与注册字段一致）。
 *
 * @param username 用户名
 * @param email 邮箱
 * @param password 明文密码
 */
public record UserCreateRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 6, max = 72) String password) {}

