package com.meng.lovespace.ai.rag.retrieve;

import com.meng.lovespace.ai.api.LoveQaKeywordRetriever;
import com.meng.lovespace.ai.dto.LoveQaKeywordHit;
import com.meng.lovespace.ai.dto.LoveQaKeywordSearchParams;
import com.meng.lovespace.ai.rag.config.RagAiProperties;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 检索流水线：向量 (+ 可选 MySQL BM25 RRF) → 阈值 → documentId 去重 → L1 rerank → final topK。
 */
@Slf4j
@Component
public class RagRetrievalPipeline {

    private final VectorStore vectorStore;
    private final RagAiProperties ragAiProperties;
    private final RagL1Reranker l1Reranker;
    private final Optional<LoveQaKeywordRetriever> keywordRetriever;

    public RagRetrievalPipeline(
            VectorStore vectorStore,
            RagAiProperties ragAiProperties,
            RagL1Reranker l1Reranker,
            @Autowired(required = false) LoveQaKeywordRetriever keywordRetriever) {
        this.vectorStore = vectorStore;
        this.ragAiProperties = ragAiProperties;
        this.l1Reranker = l1Reranker;
        this.keywordRetriever = Optional.ofNullable(keywordRetriever);
    }

    /**
     * 执行完整检索流水线。
     *
     * @param query 用户问题
     * @param filterExpression Milvus metadata filter，可为空
     * @param coupleId 有效情侣 ID（混合检索 MySQL 侧隔离），可空
     */
    public List<Document> retrieve(String query, String filterExpression, String coupleId) {
        int finalTopK = Math.max(1, ragAiProperties.getRetrieveTopK());
        int candidateK =
                Math.max(finalTopK, Math.max(1, ragAiProperties.getRetrieveCandidateK()));
        double threshold = ragAiProperties.getSimilarityThreshold();

        SearchRequest.Builder builder = SearchRequest.builder().query(query).topK(candidateK);
        if (StringUtils.hasText(filterExpression)) {
            builder.filterExpression(filterExpression.trim());
        }

        List<Document> vectorCandidates = vectorStore.similaritySearch(builder.build());
        List<Document> merged = mergeWithKeywordIfEnabled(query, coupleId, vectorCandidates);

        List<Document> aboveThreshold = RagSimilarityFilter.filterByThreshold(merged, threshold);

        List<Document> deduped =
                ragAiProperties.isDedupeByDocumentId()
                        ? RagDocumentIdDeduplicator.dedupeKeepBestScore(aboveThreshold)
                        : aboveThreshold;

        List<Document> ranked = l1Reranker.rerank(query, deduped);

        log.debug(
                "RAG pipeline vector={} merged={} thresholdPass={} deduped={} final={} hybrid={}",
                vectorCandidates.size(),
                merged.size(),
                aboveThreshold.size(),
                deduped.size(),
                ranked.size(),
                ragAiProperties.isHybridRetrievalEnabled() && keywordRetriever.isPresent());

        return ranked;
    }

    private List<Document> mergeWithKeywordIfEnabled(
            String query, String coupleId, List<Document> vectorCandidates) {
        if (!ragAiProperties.isHybridRetrievalEnabled() || keywordRetriever.isEmpty()) {
            return vectorCandidates;
        }
        if (!StringUtils.hasText(query)) {
            return vectorCandidates;
        }

        boolean includeGlobal = ragAiProperties.isAllowGlobalFallback();
        int bm25TopK = Math.max(1, ragAiProperties.getHybridBm25TopK());
        int rrfK = Math.max(1, ragAiProperties.getHybridRrfK());

        try {
            List<LoveQaKeywordHit> keywordHits =
                    keywordRetriever
                            .get()
                            .search(
                                    new LoveQaKeywordSearchParams(
                                            query.trim(),
                                            StringUtils.hasText(coupleId) ? coupleId.trim() : null,
                                            includeGlobal,
                                            bm25TopK));
            if (keywordHits.isEmpty()) {
                return vectorCandidates;
            }
            List<Document> keywordDocs =
                    keywordHits.stream().map(LoveQaKeywordDocumentAdapter::toDocument).toList();
            return RagReciprocalRankFusion.fuse(vectorCandidates, keywordDocs, rrfK);
        } catch (Exception e) {
            log.warn("hybrid keyword retrieval failed, fallback to vector only: {}", e.getMessage());
            return vectorCandidates;
        }
    }
}
