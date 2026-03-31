package com.meng.lovespace.user.exception;

/**
 * 公共分片上传业务异常，由 {@link com.meng.lovespace.user.controller.MediaChunkExceptionHandler} 处理。
 */
public class MediaChunkBusinessException extends RuntimeException {

    private final int code;

    public MediaChunkBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
