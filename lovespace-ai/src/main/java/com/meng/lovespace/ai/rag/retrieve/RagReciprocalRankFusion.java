package com.meng.lovespace.ai.rag.retrieve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;

/**
 * Reciprocal Rank Fusion（RRF）：融合向量检索与关键词检索排名。
 *
 * <p>score(d) = Σ 1 / (k + rank_i(d))；同一 {@code documentId} 合并排名贡献，优先保留向量 chunk 实体。
 */
public final class RagReciprocalRankFusion {

    private RagReciprocalRankFusion() {}

    /**
     * @param vectorHits Milvus 向量候选（按相关度降序）
     * @param keywordHits MySQL 关键词候选（按 BM25 降序）
     * @param k RRF 常数，通常 60
     * @return 融合后文档列表（按 RRF 分降序）
     */
    public static List<Document> fuse(List<Document> vectorHits, List<Document> keywordHits, int k) {
        int rrfK = Math.max(1, k);
        Map<String, Double> scores = new HashMap<>();
        Map<String, Document> chosen = new HashMap<>();

        applyList(vectorHits, rrfK, scores, chosen, true);
        applyList(keywordHits, rrfK, scores, chosen, false);

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<Document> result = new ArrayList<>(sorted.size());
        for (Map.Entry<String, Double> entry : sorted) {
            Document doc = chosen.get(entry.getKey());
            if (doc != null) {
                result.add(doc);
            }
        }
        return result;
    }

    /**
     * @param docs 待融合文档列表
     * @param rrfK RRF 常数
     * @param scores 融合得分缓存
     * @param chosen 融合结果缓存
     * @param fromVector 是否来自向量检索
     */
    private static void applyList(
            List<Document> docs,
            int rrfK,
            Map<String, Double> scores,
            Map<String, Document> chosen,
            boolean fromVector) {
        if (docs == null || docs.isEmpty()) {
            return;
        }
        for (int rank = 0; rank < docs.size(); rank++) {
            Document doc = docs.get(rank);
            String key = fusionKey(doc);
            double add = 1.0 / (rrfK + rank + 1);
            scores.merge(key, add, Double::sum);
            Document existing = chosen.get(key);
            if (existing == null) {
                chosen.put(key, doc);
            } else if (fromVector && isKeywordDoc(existing)) {
                chosen.put(key, doc);
            } else if (!fromVector && isKeywordDoc(existing) && !isKeywordDoc(doc)) {
                chosen.put(key, doc);
            }
        }
    }

    /**
     * 融合文档 ID。
     *
     * <p>向量 chunk 优先，关键词 chunk 忽略。
     */
    static String fusionKey(Document doc) {
        if (doc.getMetadata() != null) {
            Object docId = doc.getMetadata().get("documentId");
            if (docId != null && StringUtils.hasText(docId.toString())) {
                return "doc:" + docId.toString().trim();
            }
        }
        return "chunk:" + (doc.getId() != null ? doc.getId() : Integer.toHexString(System.identityHashCode(doc)));
    }

    /**
     * 是否来自关键词检索。
     */
    static boolean isKeywordDoc(Document doc) {
        if (doc.getMetadata() == null) {
            return false;
        }
        Object source = doc.getMetadata().get(LoveQaKeywordDocumentAdapter.RETRIEVAL_SOURCE);
        return LoveQaKeywordDocumentAdapter.SOURCE_KEYWORD.equals(source);
    }
}
