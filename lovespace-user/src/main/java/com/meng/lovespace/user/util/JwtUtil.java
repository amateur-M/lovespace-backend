package com.meng.lovespace.user.util;

import com.meng.lovespace.user.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT 工具：签发、校验与从 Claims 中读取用户信息。
 *
 * <p>使用 HS256；密钥由 {@link JwtProperties#secret()} 派生（短密钥会经 SHA-256 哈希以满足长度要求）。
 */
@Component
public class JwtUtil {
    /** Claim：用户主键 ID */
    public static final String CLAIM_UID = "uid";
    /** Claim：用户名 */
    public static final String CLAIM_USERNAME = "username";
    /** Claim：邮箱 */
    public static final String CLAIM_EMAIL = "email";

    private final JwtProperties props;
    private final SecretKey key;

    /**
     * @param props JWT 配置（密钥、issuer、过期时间等）
     */
    public JwtUtil(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(deriveKeyBytes(props.secret()));
    }

    /**
     * 生成访问令牌。
     *
     * @param userId 用户 ID
     * @param username 用户名
     * @param email 邮箱
     * @return 紧凑序列化的 JWT 字符串
     */
    public String generateToken(String userId, String username, String email) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.expireSeconds());

        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(userId)
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(Map.of(CLAIM_UID, userId, CLAIM_USERNAME, username, CLAIM_EMAIL, email))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析并校验签名与有效期。
     *
     * @param token Bearer 中的 JWT 串
     * @return 已签名的 Claims 包装
     * @throws io.jsonwebtoken.JwtException 令牌无效或过期时抛出
     */
    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    /**
     * 从 Claims 读取用户 ID，优先 {@link #CLAIM_UID}，否则使用 subject。
     */
    public String getUserId(Claims claims) {
        String uid = claims.get(CLAIM_UID, String.class);
        return uid != null ? uid : claims.getSubject();
    }

    /** 从 Claims 读取用户名。 */
    public String getUsername(Claims claims) {
        return claims.get(CLAIM_USERNAME, String.class);
    }

    /** 从 Claims 读取邮箱。 */
    public String getEmail(Claims claims) {
        return claims.get(CLAIM_EMAIL, String.class);
    }

    /** 从 Claims 读取 JWT ID（用于登出黑名单）。 */
    public String getJti(Claims claims) {
        return claims.getId();
    }

    /** 过期时间（Unix 秒），无则返回 0。 */
    public long getExpiresAtEpochSeconds(Claims claims) {
        Date exp = claims.getExpiration();
        return exp == null ? 0 : exp.toInstant().getEpochSecond();
    }

    /**
     * 将配置的 secret 转为 HMAC 所需字节：支持 Base64 或 UTF-8 明文；长度不足 32 字节时做 SHA-256。
     */
    private static byte[] deriveKeyBytes(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("lovespace.jwt.secret is blank");
        }

        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ignored) {
            raw = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (raw.length >= 32) {
            return raw;
        }

        try {
            return MessageDigest.getInstance("SHA-256").digest(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive JWT key", e);
        }
    }
}

