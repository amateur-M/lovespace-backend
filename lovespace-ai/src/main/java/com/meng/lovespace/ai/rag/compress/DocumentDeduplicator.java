package com.meng.lovespace.ai.rag.compress;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

/**
 * 检索结果去重合并器。
 *
 * <p>基于简单文本相似度去重，去除高相似度的重复片段。
 */
@Slf4j
public class DocumentDeduplicator {

    /** 默认相似度阈值，超过则认为重复 */
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.85;

    private final double similarityThreshold;

    public DocumentDeduplicator() {
        this(DEFAULT_SIMILARITY_THRESHOLD);
    }

    public DocumentDeduplicator(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * 对检索结果进行去重。
     *
     * <p>策略：保留高相似度片段中得分最高的一个。
     *
     * @param docs 原始检索结果（按相关性排序）
     * @return 去重后的结果
     */
    public List<Document> deduplicate(List<Document> docs) {
        if (docs == null || docs.size() <= 1) {
            return docs;
        }

        List<Document> result = new ArrayList<>();
        for (Document doc : docs) {
            if (doc == null || doc.getText() == null) {
                continue;
            }

            // 检查是否与已保留的文档重复
            boolean isDuplicate = false;
            for (Document kept : result) {
                double similarity = calculateSimilarity(doc.getText(), kept.getText());
                if (similarity >= similarityThreshold) {
                    isDuplicate = true;
                    log.debug("Duplicate detected, similarity={}", similarity);
                    break;
                }
            }

            if (!isDuplicate) {
                result.add(doc);
            }
        }

        log.debug("Deduplicated {} documents to {}", docs.size(), result.size());
        return result;
    }

    /**
     * 计算两段文本的简单相似度（基于字符级 Jaccard）。
     *
     * @param a 文本 A
     * @param b 文本 B
     * @return 相似度 0-1
     */
    private double calculateSimilarity(String a, String b) {
        if (a == null || b == null) {
            return 0.0;
        }
        if (a.equals(b)) {
            return 1.0;
        }

        // 简化的字符级 Jaccard 相似度
        // 对于短文本效果较好，长文本建议改用语义相似度
        String shorter = a.length() < b.length() ? a : b;
        String longer = a.length() < b.length() ? b : a;

        int matchCount = 0;
        for (int i = 0; i < shorter.length(); i++) {
            char c = shorter.charAt(i);
            if (longer.indexOf(c) >= 0) {
                matchCount++;
            }
        }

        // Jaccard = |A ∩ B| / |A ∪ B|
        // 简化为：匹配字符数 / 较长文本长度
        return (double) matchCount / longer.length();
    }
}
