package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理端/种子创建用户请求。
 *
 * @param phone 登录手机号
 * @param username 用户名
 * @param email 邮箱（可选，非空时需为合法邮箱）
 * @param password 明文密码
 */
public record UserCreateRequest(
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 50) String username,
        @Size(max = 100) String email,
        @NotBlank @Size(min = 6, max = 72) String password) {}
