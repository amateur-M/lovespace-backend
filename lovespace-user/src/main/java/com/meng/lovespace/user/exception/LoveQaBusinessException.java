package com.meng.lovespace.user.exception;

import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.controller.UserGlobalExceptionHandler;

/** 恋爱问答业务异常（查看历史等场景），由 {@link UserGlobalExceptionHandler} 转换。 */
public class LoveQaBusinessException extends ApiBusinessException {

    public LoveQaBusinessException(int code, String message) {
        super(code, message);
    }
}
