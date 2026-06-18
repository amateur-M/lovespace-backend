package com.meng.lovespace.user.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.admin.AdminAuthSupport;
import com.meng.lovespace.user.entity.Plan;
import com.meng.lovespace.user.entity.PlanExpense;
import com.meng.lovespace.user.entity.PlanTask;
import com.meng.lovespace.user.service.admin.AdminPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/plans")
public class AdminPlanController {

    private final AdminPlanService adminPlanService;

    public AdminPlanController(AdminPlanService adminPlanService) {
        this.adminPlanService = adminPlanService;
    }

    @GetMapping
    public ApiResponse<IPage<Plan>> pagePlans(
            Authentication auth,
            @RequestParam(value = "coupleId", required = false) String coupleId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminPlanService.pagePlans(coupleId, page, pageSize));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePlan(Authentication auth, @PathVariable("id") String id) {
        var admin = AdminAuthSupport.requireAdmin(auth);
        adminPlanService.deletePlan(admin.userId(), id);
        return ApiResponse.ok();
    }

    @GetMapping("/tasks")
    public ApiResponse<IPage<PlanTask>> pageTasks(
            Authentication auth,
            @RequestParam(value = "planId", required = false) String planId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminPlanService.pageTasks(planId, page, pageSize));
    }

    @GetMapping("/expenses")
    public ApiResponse<IPage<PlanExpense>> pageExpenses(
            Authentication auth,
            @RequestParam(value = "planId", required = false) String planId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminPlanService.pageExpenses(planId, page, pageSize));
    }
}
