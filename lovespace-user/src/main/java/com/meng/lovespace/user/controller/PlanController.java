package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.PlanCreateRequest;
import com.meng.lovespace.user.dto.PlanExpenseCreateRequest;
import com.meng.lovespace.user.dto.PlanExpenseResponse;
import com.meng.lovespace.user.dto.PlanExpenseReplaceRequest;
import com.meng.lovespace.user.dto.PlanResponse;
import com.meng.lovespace.user.dto.PlanTaskCreateRequest;
import com.meng.lovespace.user.dto.PlanTaskReplaceRequest;
import com.meng.lovespace.user.dto.PlanTaskResponse;
import com.meng.lovespace.user.dto.PlanUpdateRequest;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.PlanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 共同计划 HTTP 接口：计划 CRUD 与子任务创建、完成。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    public ApiResponse<PlanResponse> createPlan(Authentication auth, @Valid @RequestBody PlanCreateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("plan.api.create userId={} coupleId={}", p.userId(), req.coupleId());
        return ApiResponse.ok(planService.createPlan(p.userId(), req));
    }

    @GetMapping
    public ApiResponse<List<PlanResponse>> listPlans(
            Authentication auth, @RequestParam("coupleId") @NotBlank(message = "coupleId is required") String coupleId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("plan.api.list userId={} coupleId={}", p.userId(), coupleId);
        return ApiResponse.ok(planService.listPlans(p.userId(), coupleId));
    }

    @PutMapping("/{id}")
    public ApiResponse<PlanResponse> updatePlan(
            Authentication auth, @PathVariable("id") String id, @Valid @RequestBody PlanUpdateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("plan.api.update userId={} planId={}", p.userId(), id);
        return ApiResponse.ok(planService.updatePlan(p.userId(), id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePlan(Authentication auth, @PathVariable("id") String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("plan.api.delete userId={} planId={}", p.userId(), id);
        planService.deletePlan(p.userId(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/tasks")
    public ApiResponse<PlanTaskResponse> createTask(
            Authentication auth,
            @PathVariable("id") String id,
            @Valid @RequestBody PlanTaskCreateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("plan.api.task.create userId={} planId={}", p.userId(), id);
        return ApiResponse.ok(planService.createTask(p.userId(), id, req));
    }

    @PutMapping("/{id}/tasks/{taskId}")
    public ApiResponse<PlanTaskResponse> updateTask(
            Authentication auth,
            @PathVariable("id") String id,
            @PathVariable("taskId") String taskId,
            @Valid @RequestBody PlanTaskReplaceRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("plan.api.task.update userId={} planId={} taskId={}", p.userId(), id, taskId);
        return ApiResponse.ok(planService.updateTask(p.userId(), id, taskId, req));
    }

    @DeleteMapping("/{id}/tasks/{taskId}")
    public ApiResponse<Void> deleteTask(
            Authentication auth, @PathVariable("id") String id, @PathVariable("taskId") String taskId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("plan.api.task.delete userId={} planId={} taskId={}", p.userId(), id, taskId);
        planService.deleteTask(p.userId(), id, taskId);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/expenses")
    public ApiResponse<List<PlanExpenseResponse>> listPlanExpenses(
            Authentication auth, @PathVariable("id") String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("plan.api.expenses.list userId={} planId={}", p.userId(), id);
        return ApiResponse.ok(planService.listPlanExpenses(p.userId(), id));
    }

    @PostMapping("/{id}/expenses")
    public ApiResponse<PlanExpenseResponse> createPlanExpense(
            Authentication auth,
            @PathVariable("id") String id,
            @Valid @RequestBody PlanExpenseCreateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("plan.api.expenses.create userId={} planId={}", p.userId(), id);
        return ApiResponse.ok(planService.createPlanExpense(p.userId(), id, req));
    }

    @PutMapping("/{id}/expenses/{expenseId}")
    public ApiResponse<PlanExpenseResponse> updatePlanExpense(
            Authentication auth,
            @PathVariable("id") String id,
            @PathVariable("expenseId") String expenseId,
            @Valid @RequestBody PlanExpenseReplaceRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("plan.api.expenses.update userId={} planId={} expenseId={}", p.userId(), id, expenseId);
        return ApiResponse.ok(planService.updatePlanExpense(p.userId(), id, expenseId, req));
    }

    @DeleteMapping("/{id}/expenses/{expenseId}")
    public ApiResponse<Void> deletePlanExpense(
            Authentication auth,
            @PathVariable("id") String id,
            @PathVariable("expenseId") String expenseId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("plan.api.expenses.delete userId={} planId={} expenseId={}", p.userId(), id, expenseId);
        planService.deletePlanExpense(p.userId(), id, expenseId);
        return ApiResponse.ok();
    }
}
