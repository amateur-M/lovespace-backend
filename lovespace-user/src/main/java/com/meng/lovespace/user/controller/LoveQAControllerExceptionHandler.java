package com.meng.lovespace.user.controller;

import com.meng.lovespace.ai.exception.LoveQaConversationAccessException;
import com.meng.lovespace.ai.exception.LoveQaConversationNotFoundException;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.exception.LoveQaBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 恋爱知识库 RAG：会话不存在或越权；历史查询业务码。 */
@Slf4j
@RestControllerAdvice(assignableTypes = {LoveQAController.class, LoveQAHistoryController.class})
public class LoveQAControllerExceptionHandler {

    @ExceptionHandler(LoveQaConversationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> notFound(LoveQaConversationNotFoundException e) {
        log.debug("love-qa conversation not found: {}", e.getMessage());
        return ApiResponse.error(40491, e.getMessage() != null ? e.getMessage() : "会话不存在或已过期");
    }

    @ExceptionHandler(LoveQaConversationAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> forbidden(LoveQaConversationAccessException e) {
        log.warn("love-qa conversation access denied: {}", e.getMessage());
        return ApiResponse.error(40391, e.getMessage() != null ? e.getMessage() : "无权访问该会话");
    }

    @ExceptionHandler(LoveQaBusinessException.class)
    public ApiResponse<Void> loveQaBusiness(LoveQaBusinessException e) {
        log.warn("love-qa business code={} message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
}
