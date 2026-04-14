package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求体：手机号 + 密码。
 *
 * @param phone 手机号
 * @param password 明文密码（由 HTTPS 保护传输）
 */
public record LoginRequest(
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(min = 6, max = 72) String password) {}
