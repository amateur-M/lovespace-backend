package com.meng.lovespace.user.exception;

import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.controller.UserGlobalExceptionHandler;

/**
 * 情侣绑定业务流程错误，由 {@link UserGlobalExceptionHandler} 转为统一 {@code ApiResponse}。
 */
public class CoupleBindingBusinessException extends ApiBusinessException {

    /**
     * @param code 业务错误码（非 0）
     * @param message 面向调用方/前端的说明（勿含敏感信息）
     */
    public CoupleBindingBusinessException(int code, String message) {
        super(code, message);
    }
}
