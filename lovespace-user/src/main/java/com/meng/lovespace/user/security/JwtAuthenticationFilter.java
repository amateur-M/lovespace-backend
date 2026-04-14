package com.meng.lovespace.user.security;

import com.meng.lovespace.user.config.LovespaceSessionProperties;
import com.meng.lovespace.user.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 从 {@code Authorization: Bearer} 解析 JWT，校验签名与黑名单后写入 {@link SecurityContextHolder}。
 *
 * <p>启用分布式 Session 时：若 Bearer 已过期/无效，不直接 401，以便沿用 {@link org.springframework.security.web.context.SecurityContextHolderFilter}
 * 已从 Session 恢复的登录态（刷新页时常见：localStorage 里仍是旧 JWT，但 Cookie Session 仍有效）。
 *
 * <p>登录、注册等路径在 {@link #shouldNotFilter} 中跳过，避免干扰公开接口。
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> SKIP_PREFIXES =
            List.of(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/logout",
                    "/ws/",
                    "/v3/api-docs",
                    "/swagger-ui",
                    "/swagger-ui.html",
                    "/health");

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklist;
    private final LovespaceSessionProperties sessionProperties;

    /**
     * @param jwtUtil JWT 解析工具
     * @param blacklist 登出黑名单
     * @param sessionProperties 分布式 Session 开关（决定是否允许 JWT 失败后回退 Session）
     */
    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            TokenBlacklistService blacklist,
            LovespaceSessionProperties sessionProperties) {
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
        this.sessionProperties = sessionProperties;
    }

    /**
     * 公开接口前缀不匹配本过滤器逻辑时跳过。
     *
     * @param request 当前请求
     * @return true 表示不执行本 Filter
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * 若带 Bearer Token：校验、黑名单检查、设置认证；签名过期等失败时，若已启用分布式 Session 则放行链路由 Session 认证。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length()).trim();
            try {
                Jws<Claims> jws = jwtUtil.parseAndValidate(token);
                Claims claims = jws.getPayload();
                String jti = jwtUtil.getJti(claims);
                if (blacklist.isBlacklisted(jti)) {
                    log.warn("jwt rejected path={} reason=blacklisted jti={}", request.getRequestURI(), jti);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                JwtUserPrincipal principal =
                        new JwtUserPrincipal(
                                jwtUtil.getUserId(claims),
                                jwtUtil.getUsername(claims),
                                jwtUtil.getPhone(claims));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, List.of());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug(
                        "jwt authenticated path={} userId={} username={}",
                        request.getRequestURI(),
                        principal.userId(),
                        principal.username());
            } catch (Exception e) {
                log.warn(
                        "jwt invalid path={} msg={}",
                        request.getRequestURI(),
                        e.getClass().getSimpleName() + ": " + e.getMessage());
                if (sessionProperties.getDistributed().isEnabled()) {
                    log.debug(
                            "jwt invalid but distributed session enabled, continuing chain for session auth path={}",
                            request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

