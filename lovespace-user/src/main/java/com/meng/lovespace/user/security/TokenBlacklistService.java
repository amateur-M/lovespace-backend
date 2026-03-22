package com.meng.lovespace.user.security;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * JWT 登出黑名单：以 jti 为键写入 Redis，TTL 与 token 剩余有效期对齐。
 */
@Slf4j
@Service
public class TokenBlacklistService {
    private static final String PREFIX = "lovespace:jwt:blacklist:";

    private final StringRedisTemplate redis;

    /** @param redis Redis 字符串模板 */
    public TokenBlacklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 将 jti 标记为已失效。
     *
     * @param jti JWT ID
     * @param ttl 存活时间，非正则忽略
     */
    public void blacklist(String jti, Duration ttl) {
        if (jti == null || jti.isBlank()) return;
        if (ttl.isNegative() || ttl.isZero()) return;
        redis.opsForValue().set(PREFIX + jti, "1", ttl);
        log.debug("jwt blacklisted jtiPrefix={} ttlSeconds={}", jti.substring(0, Math.min(8, jti.length())), ttl.getSeconds());
    }

    /**
     * @param jti JWT ID
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) return false;
        Boolean exists = redis.hasKey(PREFIX + jti);
        return Boolean.TRUE.equals(exists);
    }
}

