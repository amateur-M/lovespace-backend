package com.meng.lovespace.user.exception;

/**
 * 恋爱时间轴业务异常，由 {@link com.meng.lovespace.user.controller.TimelineControllerExceptionHandler} 转为统一响应。
 */
public class TimelineBusinessException extends RuntimeException {

    private final int code;

    public TimelineBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
