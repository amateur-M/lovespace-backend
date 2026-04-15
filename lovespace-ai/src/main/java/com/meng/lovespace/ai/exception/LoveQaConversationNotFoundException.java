package com.meng.lovespace.ai.exception;

/** 客户端传入的 conversationId 在 Redis 中不存在或已过期。 */
public class LoveQaConversationNotFoundException extends RuntimeException {

    public LoveQaConversationNotFoundException(String conversationId) {
        super("会话不存在或已过期: " + conversationId);
    }
}
