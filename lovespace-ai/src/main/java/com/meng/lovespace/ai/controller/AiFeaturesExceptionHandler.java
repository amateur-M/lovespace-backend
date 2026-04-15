package com.meng.lovespace.ai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.meng.lovespace.common.web.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * lovespace-ai 控制器异常映射（恋爱 RAG、旅游规划等）。
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {TravelPlannerController.class, AiChatController.class})
public class AiFeaturesExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> illegalState(IllegalStateException e) {
        log.warn("AI feature unavailable: {}", e.getMessage());
        return ApiResponse.error(50301, e.getMessage() != null ? e.getMessage() : "AI 服务不可用");
    }

    @ExceptionHandler(JsonProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiResponse<Void> jsonBadGateway(JsonProcessingException e) {
        log.warn("AI JSON parse failed: {}", e.getMessage());
        return ApiResponse.error(50201, "模型返回非合法 JSON，请重试");
    }
}
