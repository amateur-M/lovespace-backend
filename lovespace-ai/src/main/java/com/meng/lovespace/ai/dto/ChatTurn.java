package com.meng.lovespace.ai.dto;

/**
 * 多轮对话中的一条消息，供 {@link com.meng.lovespace.ai.provider.LLMProvider#chatWithSystemAndHistory} 使用。
 *
 * @param role {@code user} 或 {@code assistant}（大小写不敏感）
 * @param content 正文
 */
public record ChatTurn(String role, String content) {}
