package com.meng.lovespace.ai.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meng.lovespace.ai.rag.config.RagAiProperties;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 恋爱问答多轮会话：Redis 存储 JSON，键 {@code lovespace:love-qa:conv:{conversationId}}。
 *
 * <p>由 {@link com.meng.lovespace.ai.rag.config.LoveQaRagBeansConfiguration} 注册为 Spring Bean。
 */
@Slf4j
@RequiredArgsConstructor
public class LoveQAConversationStore {

    private static final String KEY_PREFIX = "lovespace:love-qa:conv:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RagAiProperties ragAiProperties;

    public Optional<LoveQAConversationState> find(String conversationId) {
        String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + conversationId);
        if (json == null || json.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, LoveQAConversationState.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse love-qa conversation: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void save(String conversationId, LoveQAConversationState state) {
        long ttl = Math.max(60L, ragAiProperties.getConversationTtlSeconds());
        try {
            String json = objectMapper.writeValueAsString(state);
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + conversationId, json, Duration.ofSeconds(ttl));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize conversation failed", e);
        }
    }
}
