package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求体。
 *
 * @param username 用户名
 * @param email 邮箱
 * @param password 明文密码
 */
public record RegisterRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 6, max = 72) String password) {}

