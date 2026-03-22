package com.meng.lovespace.user.exception;

/**
 * 情侣绑定业务流程错误，由 {@link com.meng.lovespace.user.controller.CoupleControllerExceptionHandler} 转为统一 {@code ApiResponse}。
 */
public class CoupleBindingBusinessException extends RuntimeException {

    private final int code;

    /**
     * @param code 业务错误码（非 0）
     * @param message 面向调用方/前端的说明（勿含敏感信息）
     */
    public CoupleBindingBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** @return 业务错误码 */
    public int getCode() {
        return code;
    }
}
