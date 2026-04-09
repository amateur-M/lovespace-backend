package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.exception.LoveLetterBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将 {@link LoveLetterBusinessException} 转为统一 API 响应。 */
@Slf4j
@RestControllerAdvice(assignableTypes = {LoveLetterController.class})
public class LoveLetterControllerExceptionHandler {

    @ExceptionHandler(LoveLetterBusinessException.class)
    public ApiResponse<Void> onLoveLetter(LoveLetterBusinessException e) {
        log.warn("love letter business error code={} message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
}
