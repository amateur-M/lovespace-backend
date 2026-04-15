package com.meng.lovespace.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 情侣旅游规划请求。
 *
 * @param destination 目的地（城市或区域）
 * @param days 行程天数
 * @param budgetMin 预算下限（可选，与货币单位由前端约定）
 * @param budgetMax 预算上限（可选）
 * @param preferences 偏好标签，如浪漫、美食、轻松
 * @param transportMode 出行方式简述，可选
 */
public record TravelPlanRequest(
        @NotBlank(message = "destination 不能为空") String destination,
        @NotNull @Min(1) @Max(30) Integer days,
        Integer budgetMin,
        Integer budgetMax,
        List<String> preferences,
        String transportMode) {}
