package com.meng.lovespace.ai.rag.retrieve;

import com.meng.lovespace.ai.rag.config.RagAiProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 两阶段召回流水线：Milvus candidate-k → 阈值过滤 → documentId 去重 → L1 rerank → final topK。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagRetrievalPipeline {

    private final VectorStore vectorStore;
    private final RagAiProperties ragAiProperties;
    private final RagL1Reranker l1Reranker;

    /**
     * 执行完整检索流水线。
     *
     * @param query 用户问题
     * @param filterExpression Milvus metadata filter，可为空
     * @return final topK 文档；无命中时返回空列表
     */
    public List<Document> retrieve(String query, String filterExpression) {
        int finalTopK = Math.max(1, ragAiProperties.getRetrieveTopK());
        int candidateK =
                Math.max(finalTopK, Math.max(1, ragAiProperties.getRetrieveCandidateK()));
        double threshold = ragAiProperties.getSimilarityThreshold();

        SearchRequest.Builder builder = SearchRequest.builder().query(query).topK(candidateK);
        if (StringUtils.hasText(filterExpression)) {
            builder.filterExpression(filterExpression.trim());
        }

        List<Document> candidates = vectorStore.similaritySearch(builder.build());
        List<Document> aboveThreshold = RagSimilarityFilter.filterByThreshold(candidates, threshold);

        List<Document> deduped =
                ragAiProperties.isDedupeByDocumentId()
                        ? RagDocumentIdDeduplicator.dedupeKeepBestScore(aboveThreshold)
                        : aboveThreshold;

        List<Document> ranked = l1Reranker.rerank(query, deduped);

        log.debug(
                "RAG pipeline candidate={} thresholdPass={} deduped={} final={}",
                candidates.size(),
                aboveThreshold.size(),
                deduped.size(),
                ranked.size());

        return ranked;
    }
}
