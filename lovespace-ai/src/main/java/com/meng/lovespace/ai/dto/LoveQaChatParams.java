package com.meng.lovespace.ai.dto;

/**
 * 恋爱问答单次请求参数（由 Controller 从 JWT 与请求体组装）。
 *
 * @param userId 当前用户 ID（字符串，与 JWT 一致）
 * @param coupleId 有效情侣绑定 ID（由 {@code LoveQaChatRetrievalValidator} 解析），可空表示 GLOBAL 检索
 * @param conversationId 可选；为空则开启新会话并返回新 ID
 * @param message 本轮用户输入
 * @param retrievalFilterExpression Milvus metadata filter（Spring AI Expression Language）
 */
public record LoveQaChatParams(
        String userId,
        String coupleId,
        String conversationId,
        String message,
        String retrievalFilterExpression) {}
