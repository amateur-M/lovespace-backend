package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.exception.MessageBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 私密消息业务异常统一处理。
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {MessageController.class})
public class MessageControllerExceptionHandler {

    @ExceptionHandler(MessageBusinessException.class)
    public ApiResponse<Void> onMessage(MessageBusinessException e) {
        log.warn("message business error code={} message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
}
