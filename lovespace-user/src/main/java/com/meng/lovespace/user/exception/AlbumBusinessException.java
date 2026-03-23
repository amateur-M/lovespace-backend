package com.meng.lovespace.user.exception;

/**
 * 相册业务异常，由 {@link com.meng.lovespace.user.controller.AlbumControllerExceptionHandler} 转为统一响应。
 */
public class AlbumBusinessException extends RuntimeException {

    private final int code;

    public AlbumBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 非 0 业务码，与 {@link com.meng.lovespace.common.web.ApiResponse} 约定一致。 */
    public int getCode() {
        return code;
    }
}
