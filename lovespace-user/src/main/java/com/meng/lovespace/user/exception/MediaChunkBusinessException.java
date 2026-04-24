package com.meng.lovespace.user.exception;

import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.controller.UserGlobalExceptionHandler;

/**
 * 公共分片上传业务异常，由 {@link UserGlobalExceptionHandler} 处理。
 */
public class MediaChunkBusinessException extends ApiBusinessException {

    public MediaChunkBusinessException(int code, String message) {
        super(code, message);
    }
}
