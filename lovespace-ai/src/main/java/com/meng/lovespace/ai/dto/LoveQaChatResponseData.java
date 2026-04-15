package com.meng.lovespace.ai.dto;

/**
 * 恋爱知识库问答响应 data。
 *
 * @param reply 模型回复
 * @param conversationId 会话 ID；下轮请求需带回
 */
public record LoveQaChatResponseData(String reply, String conversationId) {}
