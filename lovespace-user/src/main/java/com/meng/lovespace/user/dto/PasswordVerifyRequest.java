package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 密码验证请求体。
 *
 * @param password 待验证的明文密码
 */
public record PasswordVerifyRequest(@NotBlank @Size(min = 6, max = 72) String password) {}
