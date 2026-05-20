package com.meng.lovespace.ai.embedding.cache;

import com.meng.lovespace.ai.config.LovespaceAiProperties;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.util.StringUtils;

/**
 * 带缓存功能的 EmbeddingModel 装饰器。
 *
 * <p>包装底层 {@link EmbeddingModel}（如 {@code DashScopeEmbeddingModel}），对 embed() 结果进行
 * Redis 缓存，降低 Embedding API 调用成本与延迟。
 *
 * <p>缓存策略：
 * <ul>
 *   <li>query 标准化（trim、lowercase、截断）后计算 SHA-256 key</li>
 *   <li>Redis 命中时直接返回，未命中时调用底层模型并缓存结果</li>
 *   <li>缓存条目包含模型版本校验，避免模型升级时混用旧缓存</li>
 * </ul>
 */
@Slf4j
public class CachedEmbeddingModel extends AbstractEmbeddingModel {

    private final EmbeddingModel delegate;
    private final EmbeddingCacheStore cacheStore;
    private final LovespaceAiProperties lovespaceAiProperties;

    public CachedEmbeddingModel(
            EmbeddingModel delegate,
            EmbeddingCacheStore cacheStore,
            LovespaceAiProperties lovespaceAiProperties) {
        this.delegate = delegate;
        this.cacheStore = cacheStore;
        this.lovespaceAiProperties = lovespaceAiProperties;
    }

    @Override
    public int dimensions() {
        int configured = lovespaceAiProperties.getEmbedding().getDimensions();
        return configured > 0 ? configured : delegate.dimensions();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        // 批量请求：逐个尝试缓存，未命中的批量调用底层
        List<String> texts = request.getInstructions();
        if (texts == null || texts.isEmpty()) {
            return delegate.call(request);
        }

        // 简化处理：直接委托给底层（批量场景当前不常用，后续可优化）
        return delegate.call(request);
    }

    @Override
    public float[] embed(String text) {
        if (!isCacheEnabled() || !StringUtils.hasText(text)) {
            return delegate.embed(text);
        }

        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return delegate.embed(text);
        }

        String cacheKey = EmbeddingCacheKeyGenerator.buildKey(
                getKeyPrefix(), normalized);

        // 1. 尝试从缓存获取
        Optional<EmbeddingCacheEntry> cached = cacheStore.get(cacheKey);
        if (cached.isPresent()) {
            EmbeddingCacheEntry entry = cached.get();
            String expectedModel = lovespaceAiProperties.getEmbedding().getModel();
            if (entry.validate(normalized, expectedModel)) {
                log.debug("Embedding cache hit: key={}, model={}, hitCount={}",
                        cacheKey, expectedModel, entry.getHitCount());
                return entry.getEmbedding();
            }
            // 校验失败（如模型版本变更），删除旧缓存
            log.debug("Embedding cache validation failed, evicting: key={}", cacheKey);
            cacheStore.evict(cacheKey);
        }

        // 2. 缓存未命中，调用底层模型
        log.debug("Embedding cache miss: key={}", cacheKey);
        float[] embedding;
        try {
            embedding = delegate.embed(text);
        } catch (Exception e) {
            log.error("Embedding delegate failed: {}", e.getMessage());
            throw e;
        }

        // 3. 写入缓存（异步或同步均可，此处同步保证下次命中）
        try {
            EmbeddingCacheEntry newEntry = EmbeddingCacheEntry.builder()
                    .originalQueryHash(EmbeddingCacheKeyGenerator.hash(normalized))
                    .embedding(embedding)
                    .model(lovespaceAiProperties.getEmbedding().getModel())
                    .dimensions(embedding.length)
                    .build();
            cacheStore.set(cacheKey, newEntry, getTtlSeconds());
        } catch (Exception e) {
            // 缓存写入失败不影响主流程
            log.warn("Failed to write embedding cache: {}", e.getMessage());
        }

        return embedding;
    }

    @Override
    public float[] embed(Document document) {
        if (document == null || !StringUtils.hasText(document.getText())) {
            return delegate.embed(document);
        }
        return embed(document.getText());
    }

    /**
     * 标准化 query 文本。
     */
    private String normalize(String text) {
        int maxLength = lovespaceAiProperties.getEmbedding().getCache().getMaxQueryLength();
        return EmbeddingCacheKeyGenerator.normalize(text, maxLength);
    }

    private boolean isCacheEnabled() {
        return lovespaceAiProperties.getEmbedding().getCache().isEnabled();
    }

    private long getTtlSeconds() {
        return lovespaceAiProperties.getEmbedding().getCache().getTtlSeconds();
    }

    private String getKeyPrefix() {
        return lovespaceAiProperties.getEmbedding().getCache().getKeyPrefix();
    }

    /**
     * 获取缓存统计信息（供监控使用）。
     *
     * @return 统计 Map
     */
    public java.util.Map<String, Long> getCacheStats() {
        return cacheStore.stats(getKeyPrefix());
    }
}
