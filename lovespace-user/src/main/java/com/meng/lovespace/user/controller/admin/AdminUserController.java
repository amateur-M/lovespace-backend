package com.meng.lovespace.user.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.admin.AdminAuthSupport;
import com.meng.lovespace.user.dto.PasswordVerifyRequest;
import com.meng.lovespace.user.dto.UserCreateRequest;
import com.meng.lovespace.user.dto.UserResponse;
import com.meng.lovespace.user.dto.admin.AdminUserUpdateRequest;
import com.meng.lovespace.user.service.admin.AdminUserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping
    public ApiResponse<UserResponse> create(
            Authentication auth, @Valid @RequestBody UserCreateRequest req) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminUserService.create(req));
    }

    @GetMapping
    public ApiResponse<IPage<UserResponse>> page(
            Authentication auth,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminUserService.page(keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(
            Authentication auth, @PathVariable("id") String id) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminUserService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
            Authentication auth,
            @PathVariable("id") String id,
            @Valid @RequestBody AdminUserUpdateRequest req) {
        var admin = AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminUserService.update(admin.userId(), id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable("id") String id) {
        var admin = AdminAuthSupport.requireAdmin(auth);
        adminUserService.delete(admin.userId(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/verify-password")
    public ApiResponse<Boolean> verifyPassword(
            Authentication auth,
            @PathVariable("id") String id,
            @RequestBody PasswordVerifyRequest request) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminUserService.verifyPassword(id, request.password()));
    }
}
