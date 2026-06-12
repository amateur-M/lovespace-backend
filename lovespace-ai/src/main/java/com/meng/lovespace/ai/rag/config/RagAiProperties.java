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

    /** Milvus 初召回条数（阈值过滤前），应 ≥ retrieveTopK。 */
    private int retrieveCandidateK = 16;

    /** 相似度阈值（0–1，COSINE distance 换算）；低于则丢弃，不进 Prompt / retrieved SSE。 */
    private double similarityThreshold = 0.55;

    /** 已绑定情侣检索时是否包含 scope=GLOBAL 公共库（false 则仅 coupleId 私有）。 */
    private boolean allowGlobalFallback = true;

    /** 检索后按 documentId 去重，同一文档仅保留最高分 chunk。 */
    private boolean dedupeByDocumentId = true;

    /** 是否启用 L1 规则重排（向量分 + category + 新近度）。 */
    private boolean rerankEnabled = true;

    /** L1 rerank：category 与 query 匹配时的加分（0–1 量级）。 */
    private double rerankCategoryBoost = 0.05;

    /** L1 rerank：新近入库文档的最大加分（0–1 量级）。 */
    private double rerankRecencyBoost = 0.05;

    /** L1 rerank：新近度加权窗口（天），超出则无 recency 加分。 */
    private int rerankRecencyDays = 90;

    /** 多轮对话在 Redis 中的 TTL（秒），默认 7 天。 */
    private long conversationTtlSeconds = 604800L;

    /** 保留的完整问答轮数（一轮 = 用户一条 + 助手一条）；超出则从最早一轮丢弃。 */
    private int maxHistoryPairs = 10;

    /** 分片策略：token（推荐，基于 TokenTextSplitter，语义更连贯） | character（兼容旧版字符滑动窗口） */
    private String chunkStrategy = "token";

    /** 是否在入库前基于内容 SHA-256 去重，避免重复文档产生冗余向量（推荐开启） */
    private boolean deduplicateOnIngest = true;

    /**
     * 是否允许未传 coupleId 时以 GLOBAL scope 入库（默认 false，须绑定情侣后以 COUPLE 入库）。
     */
    private boolean allowGlobalIngest = false;

    /** 异步入库：HTTP 立即返回 PENDING，后台分片 + Embedding + Milvus。 */
    private boolean asyncIngest = true;

    /** 文档级去重策略：REJECT（冲突 40994）| UPDATE（删旧向量后更新同 documentId）。 */
    private String documentDedupeStrategy = "REJECT";

    /** 入库文件最大字节（默认 5MB）。 */
    private int ingestMaxFileBytes = 5 * 1024 * 1024;

    /** URL 抓取响应体上限（默认 2MB）。 */
    private int ingestUrlMaxBytes = 2 * 1024 * 1024;

    /** URL 抓取超时（秒）。 */
    private int ingestUrlTimeoutSeconds = 15;

    /** 是否启用混合检索（Milvus 向量 + MySQL FULLTEXT RRF 融合）。 */
    private boolean hybridRetrievalEnabled = true;

    /** 混合检索：MySQL BM25/FULLTEXT 初召回文档数。 */
    private int hybridBm25TopK = 16;

    /** 混合检索：RRF 融合常数 k（通常 60）。 */
    private int hybridRrfK = 60;
}
