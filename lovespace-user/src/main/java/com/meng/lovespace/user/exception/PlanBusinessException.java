package com.meng.lovespace.user.exception;

/**
 * 共同计划业务异常，由 {@link com.meng.lovespace.user.controller.PlanControllerExceptionHandler} 转为统一响应。
 */
public class PlanBusinessException extends RuntimeException {

    private final int code;

    public PlanBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
