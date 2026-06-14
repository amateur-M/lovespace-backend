package com.meng.lovespace.user.service;

import com.meng.lovespace.ai.api.LoveQaKeywordRetriever;
import com.meng.lovespace.ai.dto.LoveQaKeywordHit;
import com.meng.lovespace.ai.dto.LoveQaKeywordSearchParams;
import com.meng.lovespace.user.dto.LoveQaKeywordSearchRow;
import com.meng.lovespace.user.mapper.LoveQaDocumentKeywordMapper;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** MySQL FULLTEXT 关键词检索，供 RAG 混合召回 RRF 融合。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoveQaKeywordRetrieverImpl implements LoveQaKeywordRetriever {

    private static final int PREVIEW_MAX = 800;
    private static final int MIN_QUERY_LEN = 2;
    private static final int MAX_TOP_K = 50;

    private final LoveQaDocumentKeywordMapper keywordMapper;

    @Override
    public List<LoveQaKeywordHit> search(LoveQaKeywordSearchParams params) {
        if (params == null) {
            return List.of();
        }
        String query = prepareQuery(params.query());
        if (query == null) {
            return List.of();
        }
        int limit = Math.max(1, Math.min(params.topK(), MAX_TOP_K));

        List<LoveQaKeywordSearchRow> rows = executeSearch(params, query, limit);
        if (rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().map(this::toHit).toList();
    }

    /** 执行 MySQL FULLTEXT 检索。 */
    private List<LoveQaKeywordSearchRow> executeSearch(
            LoveQaKeywordSearchParams params, String query, int limit) {
        try {
            if (!StringUtils.hasText(params.coupleId())) {
                if (!params.includeGlobal()) {
                    return List.of();
                }
                return keywordMapper.searchGlobalOnly(query, limit);
            }
            String coupleId = params.coupleId().trim();
            if (params.includeGlobal()) {
                return keywordMapper.searchCoupleWithGlobal(coupleId, query, limit);
            }
            return keywordMapper.searchCoupleOnly(coupleId, query, limit);
        } catch (Exception e) {
            log.warn("MySQL FULLTEXT search failed queryLen={}: {}", query.length(), e.getMessage());
            return List.of();
        }
    }

    private LoveQaKeywordHit toHit(LoveQaKeywordSearchRow row) {
        double score = row.getBm25Score() != null ? row.getBm25Score() : 0.0;
        String ingestedAt =
                row.getUpdatedAt() != null
                        ? row.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toString()
                        : null;
        return new LoveQaKeywordHit(
                row.getDocumentId(),
                row.getTitle(),
                row.getCategory(),
                row.getScope(),
                row.getCoupleId(),
                buildPreview(row),
                score,
                ingestedAt);
    }

    private static String buildPreview(LoveQaKeywordSearchRow row) {
        if (StringUtils.hasText(row.getContent())) {
            String text = row.getContent().strip();
            if (text.length() > PREVIEW_MAX) {
                return text.substring(0, PREVIEW_MAX) + "...";
            }
            return text;
        }
        if (StringUtils.hasText(row.getTitle())) {
            return row.getTitle().strip();
        }
        return "";
    }

    /** 去除 FULLTEXT 特殊字符并校验最小长度。 */
    static String prepareQuery(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String cleaned =
                raw.trim()
                        .replaceAll("[@+\\-><()~*\"']", " ")
                        .replaceAll("\\s+", " ")
                        .strip();
        if (cleaned.length() < MIN_QUERY_LEN) {
            return null;
        }
        return cleaned;
    }
}
