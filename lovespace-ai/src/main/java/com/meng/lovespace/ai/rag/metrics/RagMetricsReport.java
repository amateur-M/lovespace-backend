package com.meng.lovespace.ai.rag.metrics;

import lombok.Builder;
import lombok.Getter;

/**
 * RAG 流程指标报告，包含各阶段耗时。
 */
@Getter
@Builder
public class RagMetricsReport {

    /** 端到端总耗时（毫秒） */
    private final long totalMillis;

    /** Embedding 阶段耗时（毫秒），可能为 null 如果未记录 */
    private final Long embeddingMillis;

    /** 向量检索阶段耗时（毫秒），可能为 null */
    private final Long retrieveMillis;

    /** Prompt 构建阶段耗时（毫秒），可能为 null */
    private final Long promptBuildMillis;

    /** LLM 首字节延迟（毫秒），可能为 null */
    private final Long llmFirstByteMillis;

    /** LLM 完整生成耗时（毫秒），可能为 null */
    private final Long llmTotalMillis;

    /** 持久化阶段耗时（毫秒），可能为 null */
    private final Long persistMillis;

    /** Embedding 是否命中缓存 */
    private final boolean embeddingCacheHit;

    /**
     * 获取 LLM 纯生成耗时（减去首字节延迟）。
     *
     * @return 毫秒数，如果数据不完整返回 null
     */
    public Long getLlmPureGenerationMillis() {
        if (llmTotalMillis == null || llmFirstByteMillis == null) {
            return null;
        }
        return llmTotalMillis - llmFirstByteMillis;
    }

    /**
     * 格式化为日志字符串。
     *
     * @param conversationId 会话 ID
     * @return 日志字符串
     */
    public String toLogString(String conversationId) {
        StringBuilder sb = new StringBuilder();
        sb.append("RAG metrics conversationId=").append(conversationId);
        sb.append(" total=").append(totalMillis).append("ms");
        if (embeddingMillis != null) {
            sb.append(" embedding=").append(embeddingMillis).append("ms");
        }
        sb.append(" cacheHit=").append(embeddingCacheHit);
        if (retrieveMillis != null) {
            sb.append(" retrieve=").append(retrieveMillis).append("ms");
        }
        if (promptBuildMillis != null) {
            sb.append(" promptBuild=").append(promptBuildMillis).append("ms");
        }
        if (llmFirstByteMillis != null) {
            sb.append(" llmFirstByte=").append(llmFirstByteMillis).append("ms");
        }
        if (llmTotalMillis != null) {
            sb.append(" llmTotal=").append(llmTotalMillis).append("ms");
        }
        if (persistMillis != null) {
            sb.append(" persist=").append(persistMillis).append("ms");
        }
        return sb.toString();
    }
}
