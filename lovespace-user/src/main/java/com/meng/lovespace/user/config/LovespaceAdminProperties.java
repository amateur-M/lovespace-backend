package com.meng.lovespace.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 后台管理配置，绑定前缀 {@code lovespace.admin}。
 *
 * @param bootstrapPhone 首个管理员手机号（库中尚无 ADMIN 时启动提升）
 */
@ConfigurationProperties(prefix = "lovespace.admin")
public record LovespaceAdminProperties(String bootstrapPhone) {}
