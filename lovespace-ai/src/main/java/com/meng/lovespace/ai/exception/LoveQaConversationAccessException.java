package com.meng.lovespace.ai.exception;

/** 当前用户与会话归属不一致，或 coupleId 与已绑定会话冲突。 */
public class LoveQaConversationAccessException extends RuntimeException {

    public LoveQaConversationAccessException(String message) {
        super(message);
    }
}
