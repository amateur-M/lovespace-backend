package com.meng.lovespace.user.security;

/**
 * JWT 认证成功后的 {@link org.springframework.security.core.Authentication#getPrincipal()} 载体。
 *
 * @param userId 用户主键
 * @param username 用户名
 * @param email 邮箱
 */
public record JwtUserPrincipal(String userId, String username, String email) {}

