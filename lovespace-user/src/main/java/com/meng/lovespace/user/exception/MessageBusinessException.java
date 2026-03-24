package com.meng.lovespace.user.exception;

/**
 * 私密消息业务异常，由 {@link com.meng.lovespace.user.controller.MessageControllerExceptionHandler} 统一转换。
 */
public class MessageBusinessException extends RuntimeException {

    private final int code;

    public MessageBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
