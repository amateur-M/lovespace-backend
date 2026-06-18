package com.meng.lovespace.user.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.admin.AdminAuthSupport;
import com.meng.lovespace.user.entity.MemorialDay;
import com.meng.lovespace.user.service.admin.AdminMemorialService;
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
@RequestMapping("/api/v1/admin/memorial-days")
public class AdminMemorialController {

    private final AdminMemorialService adminMemorialService;

    public AdminMemorialController(AdminMemorialService adminMemorialService) {
        this.adminMemorialService = adminMemorialService;
    }

    @GetMapping
    public ApiResponse<IPage<MemorialDay>> page(
            Authentication auth,
            @RequestParam(value = "coupleId", required = false) String coupleId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminMemorialService.page(coupleId, page, pageSize));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable("id") String id) {
        var admin = AdminAuthSupport.requireAdmin(auth);
        adminMemorialService.delete(admin.userId(), id);
        return ApiResponse.ok();
    }
}
