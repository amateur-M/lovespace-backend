package com.meng.lovespace.ai.rag.retrieve;

import org.springframework.util.StringUtils;

/**
 * 恋爱问答 RAG 检索 Milvus filter 表达式构建（Spring AI Expression Language）。
 *
 * <p>规则见 memory/RAG_PHASED_OPTIMIZATION.md P2-A。
 */
public final class RagRetrievalFilterBuilder {

    private RagRetrievalFilterBuilder() {}

    /**
     * 已绑定情侣：检索本情侣私有知识，并可回退 GLOBAL 公共库。
     */
    public static String coupleWithGlobalFallback(String coupleId) {
        requireCoupleId(coupleId);
        return "(coupleId == '" + escape(coupleId) + "' || scope == 'GLOBAL')";
    }

    /** 已绑定情侣且关闭 GLOBAL 回退：仅检索本情侣私有知识。 */
    public static String coupleOnly(String coupleId) {
        requireCoupleId(coupleId);
        return "coupleId == '" + escape(coupleId) + "'";
    }

    /** 未绑定情侣且允许 GLOBAL：仅检索公共知识库。 */
    public static String globalOnly() {
        return "scope == 'GLOBAL'";
    }

    private static void requireCoupleId(String coupleId) {
        if (!StringUtils.hasText(coupleId)) {
            throw new IllegalArgumentException("coupleId 不能为空");
        }
    }

    /** 转义 filter 表达式中的单引号，防止注入。 */
    static String escape(String value) {
        return value.trim().replace("'", "\\'");
    }
}
