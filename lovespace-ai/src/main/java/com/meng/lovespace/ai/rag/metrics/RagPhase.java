package com.meng.lovespace.ai.rag.metrics;

/**
 * RAG 流程各阶段枚举，用于 Latency 分解埋点。
 */
public enum RagPhase {
    /** 整体流程开始 */
    START,

    /** Embedding 阶段：query 转为向量 */
    EMBEDDING,

    /** 向量检索阶段：Milvus similaritySearch */
    RETRIEVE,

    /** Prompt 组装阶段：system + context + history */
    PROMPT_BUILD,

    /** LLM 首字节延迟：发送请求到收到第一个 delta */
    LLM_FIRST_BYTE,

    /** LLM 完整生成：发送请求到流结束 */
    LLM_TOTAL,

    /** 结果持久化：写入 Redis / MySQL */
    PERSIST,

    /** 整体流程结束 */
    END
}
