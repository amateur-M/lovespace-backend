package com.meng.lovespace.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.meng.lovespace.user.security.JwtAuthenticationFilter;

/**
 * Spring Security：默认无状态 JWT；可选开启分布式 Session 时改为 IF_REQUIRED，与 Redis Session 配合。
 */
@Configuration
public class SecurityConfig {

    /**
     * 配置安全过滤器链：禁用 CSRF、注册 JWT 前置过滤器；Session 策略随 {@link LovespaceSessionProperties} 切换。
     *
     * @param http HttpSecurity
     * @param jwtFilter JWT 过滤器
     * @param sessionProperties 会话配置（分布式 Session 开关）
     * @return 可执行的 {@link SecurityFilterChain}
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            LovespaceSessionProperties sessionProperties)
            throws Exception {
        SessionCreationPolicy sessionPolicy =
                sessionProperties.getDistributed().isEnabled()
                        ? SessionCreationPolicy.IF_REQUIRED
                        : SessionCreationPolicy.STATELESS;
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(sessionPolicy))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/api/v1/auth/register",
                                                "/api/v1/auth/login",
                                                "/api/v1/auth/logout",
                                                "/ws/**",
                                                "/local-files/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/health/**")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

