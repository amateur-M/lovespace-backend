package com.meng.lovespace.ai.api;

import com.meng.lovespace.ai.dto.LoveQaKeywordHit;
import com.meng.lovespace.ai.dto.LoveQaKeywordSearchParams;
import java.util.List;

/**
 * 恋爱知识库 MySQL 关键词检索（BM25 / FULLTEXT），供混合检索 RRF 融合。
 *
 * <p>由 {@code lovespace-user} 实现并注册为 Spring Bean。
 */
public interface LoveQaKeywordRetriever {

    /**
     * 按 scope 过滤检索文档台账（仅 {@code SUCCESS} 状态）。
     *
     * @param params 查询与隔离参数
     * @return 按相关度降序；无命中或 FTS 不可用时返回空列表
     */
    List<LoveQaKeywordHit> search(LoveQaKeywordSearchParams params);
}
