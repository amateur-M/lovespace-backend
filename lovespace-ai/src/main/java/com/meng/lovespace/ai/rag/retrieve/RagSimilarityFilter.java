package com.meng.lovespace.ai.rag.retrieve;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.document.Document;

/**
 * 向量检索相似度阈值过滤（Milvus COSINE distance → 0–1 相似度）。
 */
public final class RagSimilarityFilter {

    private RagSimilarityFilter() {}

    /**
     * 从 Document metadata 的 {@code distance} 字段计算相似度。
     *
     * @return 相似度 0–1；无 distance 时返回 null
     */
    public static Double scoreFromDocument(Document doc) {
        if (doc == null || doc.getMetadata() == null) {
            return null;
        }
        Object scoreObj = doc.getMetadata().get("distance");
        if (scoreObj instanceof Number distance) {
            return Math.max(0, 1 - distance.doubleValue() / 2);
        }
        return null;
    }

    /**
     * 是否通过相似度阈值。无 score 元数据时保留（兼容旧向量），由 Milvus 排序决定。
     */
    public static boolean passesThreshold(Document doc, double threshold) {
        Double score = scoreFromDocument(doc);
        if (score == null) {
            return true;
        }
        return score >= threshold;
    }

    /**
     * 按 Milvus 返回顺序做阈值过滤，再取前 {@code topK} 条。
     */
    public static List<Document> filterAndLimit(List<Document> candidates, double threshold, int topK) {
        List<Document> filtered = filterByThreshold(candidates, threshold);
        if (filtered.isEmpty()) {
            return List.of();
        }
        int limit = Math.max(1, topK);
        if (filtered.size() <= limit) {
            return filtered;
        }
        return filtered.subList(0, limit);
    }

    /** 阈值过滤，不截断条数（供 dedupe / rerank 前使用）。 */
    public static List<Document> filterByThreshold(List<Document> candidates, double threshold) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<Document> result = new ArrayList<>(candidates.size());
        for (Document doc : candidates) {
            if (passesThreshold(doc, threshold)) {
                result.add(doc);
            }
        }
        return result;
    }
}
