package com.meng.lovespace.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务侧 AI 路由：选择底层 {@link com.meng.lovespace.ai.provider.LLMProvider} 实现。
 */
@Data
@ConfigurationProperties(prefix = "lovespace.ai")
public class LovespaceAiProperties {

    /**
     * 使用的提供方：{@code qwen}（通义千问，DashScope）或 {@code openai}。
     */
    private String provider = "qwen";

    /** 情侣旅游规划：POI 向量检索等。 */
    private Travel travel = new Travel();

    /** Milvus RAG 用的文本向量（默认通义 DashScope 嵌入，与 {@code spring.ai.vectorstore.milvus.embedding-dimension} 对齐）。 */
    private Embedding embedding = new Embedding();

    @Data
    public static class Embedding {
        /**
         * 向量嵌入来源：{@code dashscope}（通义 text-embedding，默认）；若使用 OpenAI 嵌入需改为 {@code openai}
         * 并取消对 {@code OpenAiEmbeddingAutoConfiguration} 的排除且配置 {@code spring.ai.openai.api-key}。
         */
        private String provider = "dashscope";

        /** DashScope 文本嵌入模型，如 {@code text-embedding-v2}。 */
        private String model = "text-embedding-v2";

        /** 向量维度，须与 {@code spring.ai.vectorstore.milvus.embedding-dimension} 一致。 */
        private int dimensions = 1536;

        /** 向量嵌入缓存配置（用于降低 Embedding API 调用成本与延迟）。 */
        private Cache cache = new Cache();
    }

    @Data
    public static class Cache {
        /** 是否启用 embedding 缓存，默认 true。 */
        private boolean enabled = true;

        /** 缓存 TTL（秒），默认 24 小时。 */
        private long ttlSeconds = 86400L;

        /** 标准化后的最大 query 长度，超过则截断，默认 500 字符。 */
        private int maxQueryLength = 500;

        /** Redis key 前缀（版本化，便于迁移），默认 lovespace:embedding:v1。 */
        private String keyPrefix = "lovespace:embedding:v1";
    }

    @Data
    public static class Travel {
        /** 是否在规划前尝试用 Milvus 检索 POI 语义片段（需 POI 向量数据入库后才有意义）。 */
        private boolean poiVectorSearchEnabled = false;
    }
}
