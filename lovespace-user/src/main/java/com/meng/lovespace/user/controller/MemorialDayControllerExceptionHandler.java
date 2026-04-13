package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.exception.MemorialDayBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将 {@link MemorialDayBusinessException} 转为统一响应。
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {MemorialDayController.class})
public class MemorialDayControllerExceptionHandler {

    @ExceptionHandler(MemorialDayBusinessException.class)
    public ApiResponse<Void> onMemorial(MemorialDayBusinessException e) {
        log.warn("memorial business error code={} message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
}
