package com.meng.lovespace.ai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.meng.lovespace.ai.dto.TravelPlanRequest;
import com.meng.lovespace.ai.travel.TravelPlanJsonSchema;
import com.meng.lovespace.ai.travel.TravelPlannerService;
import com.meng.lovespace.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 情侣旅游行程规划（JSON 结构化输出）。 */
@Tag(name = "AI", description = "情侣旅游规划")
@RestController
@RequestMapping("/api/v1/ai/travel")
@RequiredArgsConstructor
public class TravelPlannerController {

    private final TravelPlannerService travelPlannerService;

    @Operation(summary = "生成结构化旅游行程 JSON")
    @PostMapping("/plan")
    public ApiResponse<TravelPlanJsonSchema> plan(@Valid @RequestBody TravelPlanRequest request)
            throws JsonProcessingException {
        TravelPlanJsonSchema data = travelPlannerService.plan(request);
        return ApiResponse.ok(data);
    }
}
