package com.meng.lovespace.ai.embedding.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Embedding 缓存存储层，基于 Redis。
 *
 * <p>负责缓存条目的读取、写入、删除及统计查询。
 */
@Slf4j
@RequiredArgsConstructor
public class EmbeddingCacheStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 获取缓存条目。
     *
     * @param key Redis key
     * @return 缓存条目 Optional，不存在或解析失败返回 empty
     */
    public Optional<EmbeddingCacheEntry> get(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isEmpty()) {
                return Optional.empty();
            }
            EmbeddingCacheEntry entry = objectMapper.readValue(json, EmbeddingCacheEntry.class);
            entry.incrementHitCount();
            return Optional.of(entry);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse embedding cache entry for key={}: {}", key, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Redis error when getting embedding cache for key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 写入缓存条目。
     *
     * @param key Redis key
     * @param entry 缓存条目
     * @param ttlSeconds TTL（秒）
     */
    public void set(String key, EmbeddingCacheEntry entry, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(entry);
            long ttl = Math.max(60L, ttlSeconds); // 至少 60 秒
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttl));
            log.debug("Embedding cache set: key={}, ttl={}s, dimensions={}",
                    key, ttl, entry.getDimensions());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize embedding cache entry for key={}: {}", key, e.getMessage());
        } catch (Exception e) {
            log.error("Redis error when setting embedding cache for key={}: {}", key, e.getMessage());
        }
    }

    /**
     * 删除缓存条目。
     *
     * @param key Redis key
     */
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Embedding cache evicted: key={}", key);
        } catch (Exception e) {
            log.error("Redis error when evicting embedding cache for key={}: {}", key, e.getMessage());
        }
    }

    /**
     * 根据 key 前缀统计缓存信息。
     *
     * @param keyPrefix key 前缀
     * @return 统计信息：totalKeys、estimatedMemoryBytes
     */
    public Map<String, Long> stats(String keyPrefix) {
        try {
            Set<String> keys = redisTemplate.keys(keyPrefix + ":*");
            long totalKeys = keys != null ? keys.size() : 0L;
            // 粗略估算：每个 key 存储 embedding 向量（float[] 4字节/维度）+ JSON 包装开销
            long estimatedMemoryBytes = totalKeys * 1536L * 4L + totalKeys * 200L;
            return Map.of(
                    "totalKeys", totalKeys,
                    "estimatedMemoryBytes", estimatedMemoryBytes
            );
        } catch (Exception e) {
            log.error("Redis error when getting embedding cache stats: {}", e.getMessage());
            return Map.of("totalKeys", 0L, "estimatedMemoryBytes", 0L);
        }
    }

    /**
     * 清空指定前缀的所有缓存。
     *
     * @param keyPrefix key 前缀
     * @return 删除的 key 数量
     */
    public long clear(String keyPrefix) {
        try {
            Set<String> keys = redisTemplate.keys(keyPrefix + ":*");
            if (keys == null || keys.isEmpty()) {
                return 0L;
            }
            long count = redisTemplate.delete(keys);
            log.info("Embedding cache cleared: prefix={}, deletedKeys={}", keyPrefix, count);
            return count;
        } catch (Exception e) {
            log.error("Redis error when clearing embedding cache: {}", e.getMessage());
            return 0L;
        }
    }
}
