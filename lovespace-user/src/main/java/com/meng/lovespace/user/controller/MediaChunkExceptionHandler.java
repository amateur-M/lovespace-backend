package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.exception.MediaChunkBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(assignableTypes = {MediaChunkUploadController.class})
public class MediaChunkExceptionHandler {

    @ExceptionHandler(MediaChunkBusinessException.class)
    public ApiResponse<Void> onMediaChunk(MediaChunkBusinessException e) {
        log.warn("media chunk business error code={} message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
}
