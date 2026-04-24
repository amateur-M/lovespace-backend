package com.meng.lovespace.user.exception;

import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.controller.UserGlobalExceptionHandler;

/**
 * 情书生成等业务异常，由 {@link UserGlobalExceptionHandler} 转换。
 */
public class LoveLetterBusinessException extends ApiBusinessException {

    public LoveLetterBusinessException(int code, String message) {
        super(code, message);
    }
}
