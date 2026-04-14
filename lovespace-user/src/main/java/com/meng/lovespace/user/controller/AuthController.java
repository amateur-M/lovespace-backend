package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.config.LovespaceSessionProperties;
import com.meng.lovespace.user.dto.LoginRequest;
import com.meng.lovespace.user.dto.LoginResponse;
import com.meng.lovespace.user.dto.RegisterRequest;
import com.meng.lovespace.user.dto.UserResponse;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
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
    private final LovespaceSessionProperties sessionProperties;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    /**
     * @param authService 认证业务服务
     * @param sessionProperties 是否写入服务端 Session（分布式 Redis）
     */
    public AuthController(AuthService authService, LovespaceSessionProperties sessionProperties) {
        this.authService = authService;
        this.sessionProperties = sessionProperties;
    }

    /**
     * 用户注册。
     *
     * @param req 手机号、用户名、明文密码（传输层建议 HTTPS）
     * @return 注册成功返回用户信息；冲突等返回业务错误码
     */
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        log.info("auth.register phone={} username={}", req.phone(), req.username());
        try {
            UserResponse user = authService.register(req);
            log.info("auth.register success userId={}", user.id());
            return ApiResponse.ok(user);
        } catch (IllegalArgumentException e) {
            log.warn("auth.register failed phone={} reason={}", req.phone(), e.getMessage());
            return ApiResponse.error(40001, e.getMessage());
        }
    }

    /**
     * 用户登录，返回 JWT 与用户信息。
     *
     * @param req 手机号与密码
     * @return token + user；凭证错误返回 40002 等
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("auth.login phone={}", req.phone());
        try {
            LoginResponse resp = authService.login(req);
            log.info("auth.login success userId={}", resp.user().id());
            if (sessionProperties.getDistributed().isEnabled()) {
                JwtUserPrincipal principal =
                        new JwtUserPrincipal(
                                resp.user().id(), resp.user().username(), resp.user().phone());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                securityContextRepository.saveContext(context, request, response);
                log.debug("auth.login session saved userId={}", resp.user().id());
            }
            return ApiResponse.ok(resp);
        } catch (IllegalArgumentException e) {
            log.warn("auth.login failed phone={} reason={}", req.phone(), e.getMessage());
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
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            HttpServletRequest request) {
        if (auth != null && auth.startsWith("Bearer ")) {
            log.info("auth.logout (token present)");
            authService.logout(auth.substring("Bearer ".length()).trim());
        } else {
            log.info("auth.logout (no bearer token)");
        }
        if (sessionProperties.getDistributed().isEnabled()) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
                log.debug("auth.logout session invalidated");
            }
        }
        return ApiResponse.ok();
    }
}

