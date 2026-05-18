package com.meng.lovespace.ai.embedding.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meng.lovespace.ai.config.LovespaceAiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Embedding 缓存自动装配配置。
 *
 * <p>当 {@code lovespace.ai.embedding.cache.enabled=true}（默认）时，自动将 {@link CachedEmbeddingModel}
 * 注册为 {@link EmbeddingModel} 的 Primary Bean，包装原有的底层 EmbeddingModel（如 DashScopeEmbeddingModel）。
 */
@Slf4j
@Configuration
@ConditionalOnClass({EmbeddingModel.class, StringRedisTemplate.class})
@ConditionalOnBean({EmbeddingModel.class, StringRedisTemplate.class, ObjectMapper.class})
public class EmbeddingCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmbeddingCacheStore.class)
    public EmbeddingCacheStore embeddingCacheStore(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper) {
        return new EmbeddingCacheStore(stringRedisTemplate, objectMapper);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "lovespace.ai.embedding.cache",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public EmbeddingModel cachedEmbeddingModel(
            EmbeddingModel delegate,
            EmbeddingCacheStore cacheStore,
            LovespaceAiProperties lovespaceAiProperties) {
        log.info("Initializing CachedEmbeddingModel with cache enabled, " +
                        "provider={}, model={}, ttl={}s, maxQueryLength={}",
                lovespaceAiProperties.getEmbedding().getProvider(),
                lovespaceAiProperties.getEmbedding().getModel(),
                lovespaceAiProperties.getEmbedding().getCache().getTtlSeconds(),
                lovespaceAiProperties.getEmbedding().getCache().getMaxQueryLength());
        return new CachedEmbeddingModel(delegate, cacheStore, lovespaceAiProperties);
    }
}
