package com.meng.lovespace.user.dto;

/**
 * 登录成功响应。
 *
 * @param token JWT 访问令牌
 * @param user 用户公开信息
 */
public record LoginResponse(String token, UserResponse user) {}

