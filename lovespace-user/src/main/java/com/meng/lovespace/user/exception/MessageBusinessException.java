package com.meng.lovespace.user.exception;

import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.controller.UserGlobalExceptionHandler;

/**
 * 私密消息业务异常，由 {@link UserGlobalExceptionHandler} 统一转换。
 */
public class MessageBusinessException extends ApiBusinessException {

    public MessageBusinessException(int code, String message) {
        super(code, message);
    }
}
