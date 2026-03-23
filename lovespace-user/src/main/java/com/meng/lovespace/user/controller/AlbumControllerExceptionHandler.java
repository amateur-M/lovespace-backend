package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.exception.AlbumBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将 {@link AlbumBusinessException} 转为统一响应。
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {AlbumController.class})
public class AlbumControllerExceptionHandler {

    /** @param e 业务异常（含 code/message，不含敏感数据） */
    @ExceptionHandler(AlbumBusinessException.class)
    public ApiResponse<Void> onAlbum(AlbumBusinessException e) {
        log.warn("album business error code={} message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
}
