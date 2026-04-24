package com.meng.lovespace.user.exception;

import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.controller.UserGlobalExceptionHandler;

/**
 * 纪念日业务异常，由 {@link UserGlobalExceptionHandler} 转换。
 */
public class MemorialDayBusinessException extends ApiBusinessException {

    public MemorialDayBusinessException(int code, String message) {
        super(code, message);
    }
}
