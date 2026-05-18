package com.meng.lovespace.ai.embedding.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;

/**
 * Embedding 缓存 Key 生成器。
 *
 * <p>负责 query 标准化与 SHA-256 哈希计算。
 */
@Slf4j
public final class EmbeddingCacheKeyGenerator {

    private EmbeddingCacheKeyGenerator() {
        // 工具类禁止实例化
    }

    /**
     * 标准化 query：去除首尾空格、转为小写、截断至指定长度。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 标准化后的文本
     */
    public static String normalize(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String trimmed = text.trim().toLowerCase();
        if (trimmed.length() > maxLength && maxLength > 0) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }

    /**
     * 计算文本的 SHA-256 哈希。
     *
     * @param text 输入文本
     * @return 64 位十六进制哈希字符串
     */
    public static String hash(String text) {
        if (text == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * 构建 Redis 缓存 key。
     *
     * @param keyPrefix key 前缀（版本化）
     * @param normalizedQuery 标准化后的 query
     * @return 完整的 Redis key
     */
    public static String buildKey(String keyPrefix, String normalizedQuery) {
        String hash = hash(normalizedQuery);
        // 取前 16 位作为 key 主体，降低 Redis key 长度同时保持足够唯一性
        String shortHash = hash.substring(0, 16);
        return keyPrefix + ":" + shortHash;
    }
}
