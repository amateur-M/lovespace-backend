package com.meng.lovespace.ai.embedding.cache;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedding 缓存条目，存储于 Redis。
 *
 * <p>包含原始 query 哈希校验、嵌入向量、模型版本等元数据，确保缓存安全性与一致性。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingCacheEntry {

    /** 标准化后 query 的完整 SHA-256 哈希，用于校验防碰撞。 */
    private String originalQueryHash;

    /** 嵌入向量（float[] 序列化）。 */
    private float[] embedding;

    /** 生成该嵌入的模型名称，如 text-embedding-v2。 */
    private String model;

    /** 向量维度，如 1536。 */
    private int dimensions;

    /** 缓存创建时间（ISO-8601 格式）。 */
    @Builder.Default
    private String createdAt = Instant.now().toString();

    /** 缓存命中计数（可选，用于统计）。 */
    @Builder.Default
    private int hitCount = 0;

    /**
     * 验证缓存条目是否匹配给定的标准化 query 和模型。
     *
     * @param normalizedQuery 标准化后的 query
     * @param expectedModel 期望的模型名称
     * @return 校验通过返回 true，否则返回 false
     */
    public boolean validate(String normalizedQuery, String expectedModel) {
        if (normalizedQuery == null || expectedModel == null) {
            return false;
        }
        String computedHash = EmbeddingCacheKeyGenerator.hash(normalizedQuery);
        return computedHash.equals(this.originalQueryHash) && expectedModel.equals(this.model);
    }

    /**
     * 增加命中计数。
     *
     * @return 新的命中计数
     */
    public int incrementHitCount() {
        return ++this.hitCount;
    }
}
