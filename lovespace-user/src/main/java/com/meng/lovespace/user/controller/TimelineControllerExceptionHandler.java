package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.exception.TimelineBusinessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {TimelineController.class})
public class TimelineControllerExceptionHandler {

    @ExceptionHandler(TimelineBusinessException.class)
    public ApiResponse<Void> onTimeline(TimelineBusinessException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
}
