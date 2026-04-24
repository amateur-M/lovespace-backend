package com.meng.lovespace.ai.rag;

/** 一轮对话中的一条消息。 */
public record LoveQAConversationTurn(String role, String content) {}
