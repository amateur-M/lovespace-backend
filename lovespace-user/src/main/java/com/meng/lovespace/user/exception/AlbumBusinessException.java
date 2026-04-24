package com.meng.lovespace.user.exception;

import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.controller.UserGlobalExceptionHandler;

/**
 * 相册业务异常，由 {@link UserGlobalExceptionHandler} 转为统一响应。
 */
public class AlbumBusinessException extends ApiBusinessException {

    public AlbumBusinessException(int code, String message) {
        super(code, message);
    }
}
