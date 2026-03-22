package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.LoginRequest;
import com.meng.lovespace.user.dto.LoginResponse;
import com.meng.lovespace.user.dto.RegisterRequest;
import com.meng.lovespace.user.dto.UserResponse;
import com.meng.lovespace.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证相关 REST 接口：注册、登录、登出。
 *
 * <p>路径前缀：{@code /api/v1/auth}。密码等敏感信息仅记录日志中的非敏感字段。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * @param authService 认证业务服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册。
     *
     * @param req 用户名、邮箱、明文密码（传输层建议 HTTPS）
     * @return 注册成功返回用户信息；冲突等返回业务错误码
     */
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        log.info("auth.register username={} email={}", req.username(), req.email());
        try {
            UserResponse user = authService.register(req);
            log.info("auth.register success userId={}", user.id());
            return ApiResponse.ok(user);
        } catch (IllegalArgumentException e) {
            log.warn("auth.register failed username={} reason={}", req.username(), e.getMessage());
            return ApiResponse.error(40001, e.getMessage());
        }
    }

    /**
     * 用户登录，返回 JWT 与用户信息。
     *
     * @param req 邮箱与密码
     * @return token + user；凭证错误返回 40002 等
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        log.info("auth.login email={}", req.email());
        try {
            LoginResponse resp = authService.login(req);
            log.info("auth.login success userId={}", resp.user().id());
            return ApiResponse.ok(resp);
        } catch (IllegalArgumentException e) {
            log.warn("auth.login failed email={} reason={}", req.email(), e.getMessage());
            return ApiResponse.error(40002, e.getMessage());
        }
    }

    /**
     * 登出：若请求头携带 {@code Authorization: Bearer &lt;token&gt;}，则将 token 加入黑名单直至过期。
     *
     * @param auth Authorization 请求头，可为空
     * @return 始终返回成功包装（无 data）
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            log.info("auth.logout (token present)");
            authService.logout(auth.substring("Bearer ".length()).trim());
        } else {
            log.info("auth.logout (no bearer token)");
        }
        return ApiResponse.ok();
    }
}

