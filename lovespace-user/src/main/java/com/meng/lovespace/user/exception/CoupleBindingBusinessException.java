package com.meng.lovespace.user.exception;

/**
 * 情侣绑定业务流程错误，由 {@link com.meng.lovespace.user.controller.CoupleControllerExceptionHandler} 转为统一 {@code ApiResponse}。
 */
public class CoupleBindingBusinessException extends RuntimeException {

    private final int code;

    public CoupleBindingBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
