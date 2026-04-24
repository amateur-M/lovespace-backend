package com.meng.lovespace.user.controller;

import com.meng.lovespace.ai.exception.LoveQaConversationAccessException;
import com.meng.lovespace.ai.exception.LoveQaConversationNotFoundException;
import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.common.web.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * user 模块控制器统一异常映射：业务码异常、恋爱问答会话、参数校验。
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.meng.lovespace.user.controller")
public class UserGlobalExceptionHandler {

    @ExceptionHandler(ApiBusinessException.class)
    public ApiResponse<Void> onApiBusiness(ApiBusinessException e) {
        log.warn("api business error code={} message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(LoveQaConversationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> loveQaNotFound(LoveQaConversationNotFoundException e) {
        log.debug("love-qa conversation not found: {}", e.getMessage());
        return ApiResponse.error(40491, e.getMessage() != null ? e.getMessage() : "会话不存在或已过期");
    }

    @ExceptionHandler(LoveQaConversationAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> loveQaForbidden(LoveQaConversationAccessException e) {
        log.warn("love-qa conversation access denied: {}", e.getMessage());
        return ApiResponse.error(40391, e.getMessage() != null ? e.getMessage() : "无权访问该会话");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> onMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg =
                e.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        log.warn("request body validation failed: {}", msg);
        return ApiResponse.error(40001, msg.isEmpty() ? "参数校验失败" : msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> onConstraintViolation(ConstraintViolationException e) {
        String msg =
                e.getConstraintViolations().stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
        log.warn("constraint validation failed: {}", msg);
        return ApiResponse.error(40001, msg.isEmpty() ? "参数校验失败" : msg);
    }
}
