package com.meng.lovespace.ai.rag.retrieve;

import com.meng.lovespace.ai.rag.config.RagAiProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * L1 规则重排：向量相似度 + category 关键词匹配加权 + 入库新近度加权。
 */
@Component
@RequiredArgsConstructor
public class RagL1Reranker {

    private final RagAiProperties ragAiProperties;

    /**
     * 对候选片段打分重排并取 final topK。
     *
     * @param query 用户问题
     * @param docs 阈值过滤且 documentId 去重后的候选
     * @return 重排后的 topK 文档
     */
    public List<Document> rerank(String query, List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }
        if (!ragAiProperties.isRerankEnabled()) {
            return limitTopK(docs);
        }

        String queryLower = query != null ? query.toLowerCase() : "";
        List<ScoredDocument> scored = new ArrayList<>(docs.size());
        for (Document doc : docs) {
            scored.add(new ScoredDocument(doc, computeScore(queryLower, doc)));
        }
        scored.sort(Comparator.comparingDouble(ScoredDocument::score).reversed());
        int topK = Math.max(1, ragAiProperties.getRetrieveTopK());
        List<Document> result = new ArrayList<>(Math.min(topK, scored.size()));
        for (int i = 0; i < scored.size() && result.size() < topK; i++) {
            result.add(scored.get(i).doc());
        }
        return result;
    }

    private double computeScore(String queryLower, Document doc) {
        double vector = scoreOrDefault(doc, 0.5);
        double category = categoryBoost(queryLower, doc);
        double recency = recencyBoost(doc);
        return vector + category + recency;
    }

    private double categoryBoost(String queryLower, Document doc) {
        if (!StringUtils.hasText(queryLower)) {
            return 0;
        }
        String category = metadataString(doc, "category");
        if (!StringUtils.hasText(category)) {
            return 0;
        }
        String catLower = category.trim().toLowerCase();
        if (queryLower.contains(catLower) || catLower.contains(queryLower)) {
            return ragAiProperties.getRerankCategoryBoost();
        }
        return 0;
    }

    private double recencyBoost(Document doc) {
        String ingestedAt = metadataString(doc, "ingestedAt");
        if (!StringUtils.hasText(ingestedAt)) {
            return 0;
        }
        try {
            Instant instant = Instant.parse(ingestedAt.trim());
            long daysAgo = ChronoUnit.DAYS.between(instant, Instant.now());
            int windowDays = Math.max(1, ragAiProperties.getRerankRecencyDays());
            if (daysAgo >= windowDays) {
                return 0;
            }
            double ratio = 1.0 - (double) daysAgo / windowDays;
            return ragAiProperties.getRerankRecencyBoost() * ratio;
        } catch (Exception e) {
            return 0;
        }
    }

    private static double scoreOrDefault(Document doc, double defaultScore) {
        Double score = RagSimilarityFilter.scoreFromDocument(doc);
        return score != null ? score : defaultScore;
    }

    private static String metadataString(Document doc, String key) {
        if (doc.getMetadata() == null) {
            return null;
        }
        Object val = doc.getMetadata().get(key);
        return val != null ? val.toString() : null;
    }

    private List<Document> limitTopK(List<Document> docs) {
        int topK = Math.max(1, ragAiProperties.getRetrieveTopK());
        if (docs.size() <= topK) {
            return docs;
        }
        return docs.subList(0, topK);
    }

    private record ScoredDocument(Document doc, double score) {}
}
