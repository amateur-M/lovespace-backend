package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.exception.CoupleBindingBusinessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 仅处理 {@link CoupleController} 抛出的情侣绑定业务异常。
 */
@RestControllerAdvice(assignableTypes = {CoupleController.class})
public class CoupleControllerExceptionHandler {

    @ExceptionHandler(CoupleBindingBusinessException.class)
    public ApiResponse<Void> onCoupleBindingBusiness(CoupleBindingBusinessException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
}
