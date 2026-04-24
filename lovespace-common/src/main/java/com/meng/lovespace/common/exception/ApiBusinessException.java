package com.meng.lovespace.common.exception;

/**
 * 带业务错误码的 API 业务异常基类，由应用层 {@code @RestControllerAdvice} 统一转为 {@link com.meng.lovespace.common.web.ApiResponse}。
 */
public class ApiBusinessException extends RuntimeException {

    private final int code;

    /**
     * @param code 业务错误码（非 0）
     * @param message 面向调用方/前端的说明（勿含敏感信息）
     */
    public ApiBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** @return 业务错误码 */
    public int getCode() {
        return code;
    }
}
