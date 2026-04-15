package com.meng.lovespace.ai.travel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 旅游规划结构化输出（与 LLM 约定 JSON 字段一致，供 Jackson 反序列化）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelPlanJsonSchema(
        String destination,
        int days,
        String summary,
        List<DailyPlan> dailyPlans) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DailyPlan(int dayIndex, String theme, List<PlanItem> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlanItem(
            String timeSlot,
            int order,
            String title,
            String type,
            String description,
            String locationHint,
            List<String> foodRecommendations) {}
}
