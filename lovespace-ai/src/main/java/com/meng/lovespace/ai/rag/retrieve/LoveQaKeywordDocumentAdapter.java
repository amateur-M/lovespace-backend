package com.meng.lovespace.ai.rag.retrieve;

import com.meng.lovespace.ai.dto.LoveQaKeywordHit;
import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;

/** 将 MySQL 关键词命中转为 Spring AI {@link Document}（供 RRF 与 L1 rerank）。 */
public final class LoveQaKeywordDocumentAdapter {

    static final String RETRIEVAL_SOURCE = "retrievalSource";
    static final String SOURCE_KEYWORD = "KEYWORD";

    private LoveQaKeywordDocumentAdapter() {}

    public static Document toDocument(LoveQaKeywordHit hit) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("documentId", hit.documentId());
        meta.put(RETRIEVAL_SOURCE, SOURCE_KEYWORD);
        if (StringUtils.hasText(hit.title())) {
            meta.put("title", hit.title().trim());
        }
        if (StringUtils.hasText(hit.category())) {
            meta.put("category", hit.category().trim());
        }
        if (StringUtils.hasText(hit.scope())) {
            meta.put("scope", hit.scope().trim());
        }
        if (StringUtils.hasText(hit.coupleId())) {
            meta.put("coupleId", hit.coupleId().trim());
        }
        if (StringUtils.hasText(hit.ingestedAt())) {
            meta.put("ingestedAt", hit.ingestedAt().trim());
        }
        meta.put("bm25Score", hit.bm25Score());
        meta.put("distance", bm25ToSyntheticDistance(hit.bm25Score()));

        String text =
                StringUtils.hasText(hit.textPreview())
                        ? hit.textPreview()
                        : (StringUtils.hasText(hit.title()) ? hit.title() : "");
        return new Document("keyword-" + hit.documentId(), text, meta);
    }

    /** 将 BM25 分映射为 COSINE distance 占位，便于与向量分统一过滤（KEYWORD 路径在 filter 中豁免）。 */
    static double bm25ToSyntheticDistance(double bm25Score) {
        if (bm25Score <= 0) {
            return 1.0;
        }
        double sim = Math.min(1.0, bm25Score / (bm25Score + 1.0));
        return Math.max(0, 2 * (1 - sim));
    }
}
