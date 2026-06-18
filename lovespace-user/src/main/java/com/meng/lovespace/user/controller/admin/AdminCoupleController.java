package com.meng.lovespace.user.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.admin.AdminAuthSupport;
import com.meng.lovespace.user.dto.admin.AdminCoupleListItem;
import com.meng.lovespace.user.service.admin.AdminCoupleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/couples")
public class AdminCoupleController {

    private final AdminCoupleService adminCoupleService;

    public AdminCoupleController(AdminCoupleService adminCoupleService) {
        this.adminCoupleService = adminCoupleService;
    }

    @GetMapping
    public ApiResponse<IPage<AdminCoupleListItem>> page(
            Authentication auth,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminCoupleService.page(status, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminCoupleListItem> getById(
            Authentication auth, @PathVariable("id") String id) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminCoupleService.getById(id));
    }

    @PostMapping("/{id}/force-separate")
    public ApiResponse<Void> forceSeparate(
            Authentication auth, @PathVariable("id") String id) {
        var admin = AdminAuthSupport.requireAdmin(auth);
        adminCoupleService.forceSeparate(admin.userId(), id);
        return ApiResponse.ok();
    }
}
