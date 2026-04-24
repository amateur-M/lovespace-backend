package com.meng.lovespace.ai.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 恋爱问答 RAG：分片、检索与多轮会话参数（{@code lovespace.ai.rag.*}，在 Milvus 等 Bean 就绪时生效）。
 */
@Data
@ConfigurationProperties(prefix = "lovespace.ai.rag")
public class RagAiProperties {

    /** 文本分片目标长度（字符级近似，实现见 {@link com.meng.lovespace.ai.rag.DocumentIngestPipeline}）。 */
    private int chunkSize = 800;

    private int chunkOverlap = 100;

    private int retrieveTopK = 4;

    /** 多轮对话在 Redis 中的 TTL（秒），默认 7 天。 */
    private long conversationTtlSeconds = 604800L;

    /** 保留的完整问答轮数（一轮 = 用户一条 + 助手一条）；超出则从最早一轮丢弃。 */
    private int maxHistoryPairs = 10;
}
