package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.exception.TimelineBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将 {@link TimelineBusinessException} 转为 {@link ApiResponse}，并输出告警日志便于排查。
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {TimelineController.class, TimelineChunkUploadController.class})
public class TimelineControllerExceptionHandler {

    /**
     * @param e 时间轴业务异常（含业务错误码与提示文案）
     * @return 统一错误响应体
     */
    @ExceptionHandler(TimelineBusinessException.class)
    public ApiResponse<Void> onTimeline(TimelineBusinessException e) {
        log.warn("timeline business error code={} message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
}
