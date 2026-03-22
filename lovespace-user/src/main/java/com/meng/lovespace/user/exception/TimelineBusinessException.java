package com.meng.lovespace.user.exception;

/**
 * 恋爱时间轴业务异常，由 {@link com.meng.lovespace.user.controller.TimelineControllerExceptionHandler} 转为统一响应。
 */
public class TimelineBusinessException extends RuntimeException {

    private final int code;

    /**
     * @param code 业务错误码（非 0）
     * @param message 面向调用方/前端的说明（勿含敏感信息）
     */
    public TimelineBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** @return 业务错误码 */
    public int getCode() {
        return code;
    }
}
