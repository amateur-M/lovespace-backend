package com.meng.lovespace.user.exception;

import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.controller.UserGlobalExceptionHandler;

/**
 * 恋爱时间轴业务异常，由 {@link UserGlobalExceptionHandler} 转为统一响应。
 */
public class TimelineBusinessException extends ApiBusinessException {

    /**
     * @param code 业务错误码（非 0）
     * @param message 面向调用方/前端的说明（勿含敏感信息）
     */
    public TimelineBusinessException(int code, String message) {
        super(code, message);
    }
}
