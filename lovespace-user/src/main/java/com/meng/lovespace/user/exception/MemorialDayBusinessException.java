package com.meng.lovespace.user.exception;

/**
 * 纪念日业务异常，由 {@link com.meng.lovespace.user.controller.MemorialDayControllerExceptionHandler} 转换。
 */
public class MemorialDayBusinessException extends RuntimeException {

    private final int code;

    public MemorialDayBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
