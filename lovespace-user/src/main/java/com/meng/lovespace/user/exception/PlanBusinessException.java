package com.meng.lovespace.user.exception;

import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.controller.UserGlobalExceptionHandler;

/**
 * 共同计划业务异常，由 {@link UserGlobalExceptionHandler} 转为统一响应。
 */
public class PlanBusinessException extends ApiBusinessException {

    public PlanBusinessException(int code, String message) {
        super(code, message);
    }
}
