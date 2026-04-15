package com.meng.lovespace.ai.travel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meng.lovespace.ai.dto.TravelPlanRequest;
import com.meng.lovespace.ai.service.LlmRouter;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 情侣旅游行程规划：LLM 输出 JSON，可选注入 POI 语义检索摘要。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlannerService {

    private static final String JSON_SYSTEM =
            "你是资深旅行规划师。请根据用户需求输出**唯一一个**合法 JSON 对象，不要 Markdown 代码围栏，不要任何解释性文字。"
                    + " JSON 必须符合字段：destination(string), days(number), summary(string),"
                    + " dailyPlans(array of { dayIndex(number), theme(string),"
                    + " items(array of { timeSlot(string), order(number), title(string),"
                    + " type(string 取值 attraction|meal|transport), description(string),"
                    + " locationHint(string), foodRecommendations(array of string，仅餐食项可非空) }) })."
                    + " days 必须与 dailyPlans 长度一致。";

    private final LlmRouter llmRouter;
    private final TravelPoiVectorSearchService travelPoiVectorSearchService;
    private final AmapPlacesClient amapPlacesClient;
    private final ObjectMapper objectMapper;

    /**
     * 生成结构化行程 JSON 字符串（解析为 {@link TravelPlanJsonSchema} 由调用方决定）。
     *
     * @param request 用户约束
     * @return JSON 字符串
     */
    public String planAsJson(TravelPlanRequest request) {
        String prefs =
                request.preferences() == null
                        ? ""
                        : request.preferences().stream().collect(Collectors.joining(","));
        String poiHints =
                travelPoiVectorSearchService
                        .searchPoiHints(prefs + " " + request.destination(), 8)
                        .toString();
        String amapHints =
                amapPlacesClient
                        .searchHints(request.destination(), prefs)
                        .toString();
        String user =
                "目的地："
                        + request.destination()
                        + "\n天数："
                        + request.days()
                        + "\n预算："
                        + request.budgetMin()
                        + "-"
                        + request.budgetMax()
                        + "\n偏好："
                        + prefs
                        + "\n出行方式："
                        + (request.transportMode() != null ? request.transportMode() : "")
                        + "\n可选 POI 语义线索："
                        + poiHints
                        + "\n可选地图线索："
                        + amapHints;
        String raw = llmRouter.resolve().chatWithSystem(JSON_SYSTEM, user);
        try {
            objectMapper.readTree(raw);
            return raw;
        } catch (JsonProcessingException e) {
            log.warn("Travel plan JSON parse failed, returning raw text for repair pipeline: {}", e.getMessage());
            // TODO: optional one-shot repair prompt
            return raw;
        }
    }

    /**
     * 解析为强类型结构（若 JSON 不合法则抛出）。
     */
    public TravelPlanJsonSchema plan(TravelPlanRequest request) throws JsonProcessingException {
        String json = planAsJson(request);
        return objectMapper.readValue(json, TravelPlanJsonSchema.class);
    }
}
