package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求体：手机号账号 + 展示用户名 + 密码；邮箱在个人信息中维护。
 *
 * @param phone 手机号（中国大陆 11 位，可含空格）
 * @param username 用户名（展示名）
 * @param password 明文密码
 */
public record RegisterRequest(
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 6, max = 72) String password) {}
