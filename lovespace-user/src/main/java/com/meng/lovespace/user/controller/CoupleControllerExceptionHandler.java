package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.exception.CoupleBindingBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 仅处理 {@link CoupleController} 抛出的情侣绑定业务异常，并记录告警日志。
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {CoupleController.class})
public class CoupleControllerExceptionHandler {

    /**
     * @param e 情侣绑定业务异常
     * @return 统一错误响应体
     */
    @ExceptionHandler(CoupleBindingBusinessException.class)
    public ApiResponse<Void> onCoupleBindingBusiness(CoupleBindingBusinessException e) {
        log.warn("couple business error code={} message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
}
