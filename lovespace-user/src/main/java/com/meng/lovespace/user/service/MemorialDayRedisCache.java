package com.meng.lovespace.user.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meng.lovespace.user.config.LovespaceMemorialProperties;
import com.meng.lovespace.user.dto.MemorialNextResponse;
import com.meng.lovespace.user.dto.MemorialUpcomingItemResponse;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 纪念日 Redis 缓存：减轻高频轮询对 DB 的压力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemorialDayRedisCache {

    private static final String PREFIX_NEXT = "lovespace:memorial:next:";
    private static final String PREFIX_UPCOMING = "lovespace:memorial:upcoming:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final LovespaceMemorialProperties properties;

    public void putNext(String coupleId, MemorialNextResponse next) {
        String key = PREFIX_NEXT + coupleId;
        try {
            String json = objectMapper.writeValueAsString(next);
            redis.opsForValue().set(key, json, Duration.ofSeconds(properties.getCacheTtlSeconds()));
        } catch (Exception e) {
            log.warn("memorial cache putNext failed coupleId={}", coupleId, e);
        }
    }

    public MemorialNextResponse getNext(String coupleId) {
        String key = PREFIX_NEXT + coupleId;
        try {
            String json = redis.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, MemorialNextResponse.class);
        } catch (Exception e) {
            log.debug("memorial cache getNext miss or parse error coupleId={}", coupleId, e);
            return null;
        }
    }

    public void putUpcoming(String coupleId, List<MemorialUpcomingItemResponse> items) {
        String key = PREFIX_UPCOMING + coupleId;
        try {
            String json = objectMapper.writeValueAsString(items);
            redis.opsForValue().set(key, json, Duration.ofSeconds(properties.getCacheTtlSeconds()));
        } catch (Exception e) {
            log.warn("memorial cache putUpcoming failed coupleId={}", coupleId, e);
        }
    }

    public List<MemorialUpcomingItemResponse> getUpcoming(String coupleId) {
        String key = PREFIX_UPCOMING + coupleId;
        try {
            String json = redis.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.debug("memorial cache getUpcoming miss coupleId={}", coupleId, e);
            return null;
        }
    }

    public void evictCouple(String coupleId) {
        redis.delete(PREFIX_NEXT + coupleId);
        redis.delete(PREFIX_UPCOMING + coupleId);
        log.debug("memorial cache evicted coupleId={}", coupleId);
    }
}
