package com.meng.lovespace.ai.rag.retrieve;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;

/**
 * 检索结果按 {@code documentId} 去重，同一文档仅保留向量相似度最高的 chunk。
 */
public final class RagDocumentIdDeduplicator {

    private RagDocumentIdDeduplicator() {}

    /**
     * 按 Milvus 返回顺序遍历，每个 documentId 保留最高相似度片段。
     *
     * @param docs 已通过阈值过滤的候选列表（通常按相似度降序）
     * @return 去重后的列表（顺序为各 documentId 首次出现位置）
     */
    public static List<Document> dedupeKeepBestScore(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }
        Map<String, Document> bestByDocId = new LinkedHashMap<>();
        for (Document doc : docs) {
            String key = documentKey(doc);
            Document existing = bestByDocId.get(key);
            if (existing == null || compareScore(doc, existing) > 0) {
                bestByDocId.put(key, doc);
            }
        }
        return new ArrayList<>(bestByDocId.values());
    }

    private static int compareScore(Document a, Document b) {
        double sa = scoreOrZero(a);
        double sb = scoreOrZero(b);
        return Double.compare(sa, sb);
    }

    private static double scoreOrZero(Document doc) {
        Double score = RagSimilarityFilter.scoreFromDocument(doc);
        return score != null ? score : 0.0;
    }

    private static String documentKey(Document doc) {
        if (doc.getMetadata() != null) {
            Object docId = doc.getMetadata().get("documentId");
            if (docId != null && StringUtils.hasText(docId.toString())) {
                return "doc:" + docId.toString().trim();
            }
        }
        return "chunk:" + (doc.getId() != null ? doc.getId() : System.identityHashCode(doc));
    }
}
