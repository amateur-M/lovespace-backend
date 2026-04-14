package com.meng.lovespace.user.service;

import com.meng.lovespace.user.dto.LoginRequest;
import com.meng.lovespace.user.dto.LoginResponse;
import com.meng.lovespace.user.dto.RegisterRequest;
import com.meng.lovespace.user.dto.UserResponse;

/**
 * 认证领域服务：注册、登录、登出（黑名单）。
 */
public interface AuthService {

    /**
     * 注册新用户，手机号与用户名需唯一。
     *
     * @param req 注册参数
     * @return 用户公开信息
     * @throws IllegalArgumentException 业务校验失败（如重复）
     */
    UserResponse register(RegisterRequest req);

    /**
     * 手机号密码登录，签发 JWT。
     *
     * @param req 登录参数
     * @return token 与用户信息
     * @throws IllegalArgumentException 凭证错误或用户禁用
     */
    LoginResponse login(LoginRequest req);

    /**
     * 将当前 token 的 jti 写入 Redis 黑名单直至过期。
     *
     * @param token 原始 JWT 字符串（不含 Bearer 前缀）
     */
    void logout(String token);
}
