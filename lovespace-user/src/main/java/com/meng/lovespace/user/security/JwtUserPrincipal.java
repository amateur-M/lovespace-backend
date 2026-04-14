package com.meng.lovespace.user.security;

import java.io.Serializable;

/**
 * JWT 认证成功后的 {@link org.springframework.security.core.Authentication#getPrincipal()} 载体。
 *
 * @param userId 用户主键
 * @param username 用户名
 * @param phone 登录手机号
 */
public record JwtUserPrincipal(String userId, String username, String phone) implements Serializable {
    private static final long serialVersionUID = 1L;
}
