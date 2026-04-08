package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.EmotionAnalysisReport;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.EmotionAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 恋爱记录情感分析：统计 + 通义千问解读。
 */
@Slf4j
@Validated
@Tag(name = "AI Emotion", description = "情感分析")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class EmotionAnalysisController {

    private final EmotionAnalysisService emotionAnalysisService;

    /**
     * 获取指定情侣、时间区间内的情感分析报告。
     *
     * <p>未传 {@code startDate}/{@code endDate} 时，默认分析「结束日（默认今天）」起向前 30 天。
     */
    @Operation(summary = "情感分析报告")
    @GetMapping("/emotion")
    public ApiResponse<EmotionAnalysisReport> getEmotionReport(
            Authentication auth,
            @RequestParam("coupleId") @NotBlank(message = "coupleId is required") String coupleId,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("ai.emotion userId={} coupleId={} startDate={} endDate={}", p.userId(), coupleId, startDate, endDate);
        return ApiResponse.ok(emotionAnalysisService.analyze(p.userId(), coupleId, startDate, endDate));
    }
}
