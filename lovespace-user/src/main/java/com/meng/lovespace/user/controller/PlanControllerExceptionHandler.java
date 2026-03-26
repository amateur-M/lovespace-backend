package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.exception.PlanBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将 {@link PlanBusinessException} 转为统一响应。
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {PlanController.class})
public class PlanControllerExceptionHandler {

    @ExceptionHandler(PlanBusinessException.class)
    public ApiResponse<Void> onPlan(PlanBusinessException e) {
        log.warn("plan business error code={} message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
}
