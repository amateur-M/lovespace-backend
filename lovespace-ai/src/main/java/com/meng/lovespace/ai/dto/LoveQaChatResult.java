package com.meng.lovespace.ai.dto;

/**
 * 恋爱问答响应：模型回复与当前会话 ID（前端需在下轮原样传回 conversationId）。
 */
public record LoveQaChatResult(String reply, String conversationId) {}
