package com.meng.lovespace.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 相关配置，绑定前缀 {@code lovespace.jwt}。
 *
 * @param secret 签名密钥（可为 Base64 或明文，过短会在 {@link com.meng.lovespace.user.util.JwtUtil} 中派生）
 * @param issuer 签发者
 * @param expireSeconds 过期秒数
 */
@ConfigurationProperties(prefix = "lovespace.jwt")
public record JwtProperties(String secret, String issuer, long expireSeconds) {}

