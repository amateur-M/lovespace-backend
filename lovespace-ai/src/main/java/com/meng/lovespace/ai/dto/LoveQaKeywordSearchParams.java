package com.meng.lovespace.ai.dto;

/**
 * MySQL FULLTEXT 关键词检索参数。
 *
 * @param query 用户问题
 * @param coupleId 有效情侣 ID；GLOBAL 模式可为 null
 * @param includeGlobal 是否包含 scope=GLOBAL 公共库
 * @param topK 最大返回文档数
 */
public record LoveQaKeywordSearchParams(
        String query, String coupleId, boolean includeGlobal, int topK) {}
