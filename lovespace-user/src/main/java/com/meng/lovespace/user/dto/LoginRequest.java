package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求体。
 *
 * @param email 邮箱
 * @param password 明文密码（由 HTTPS 保护传输）
 */
public record LoginRequest(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 6, max = 72) String password) {}

