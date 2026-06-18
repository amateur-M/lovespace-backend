package com.meng.lovespace.user.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.admin.AdminAuthSupport;
import com.meng.lovespace.user.entity.PrivateMessage;
import com.meng.lovespace.user.service.admin.AdminMessageService;
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
@RequestMapping("/api/v1/admin/messages")
public class AdminMessageController {

    private final AdminMessageService adminMessageService;

    public AdminMessageController(AdminMessageService adminMessageService) {
        this.adminMessageService = adminMessageService;
    }

    @GetMapping
    public ApiResponse<IPage<PrivateMessage>> page(
            Authentication auth,
            @RequestParam(value = "coupleId", required = false) String coupleId,
            @RequestParam(value = "senderId", required = false) String senderId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminMessageService.page(coupleId, senderId, page, pageSize));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable("id") String id) {
        var admin = AdminAuthSupport.requireAdmin(auth);
        adminMessageService.delete(admin.userId(), id);
        return ApiResponse.ok();
    }
}
