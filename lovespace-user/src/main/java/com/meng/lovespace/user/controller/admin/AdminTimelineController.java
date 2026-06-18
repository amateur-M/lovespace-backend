package com.meng.lovespace.user.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.admin.AdminAuthSupport;
import com.meng.lovespace.user.entity.LoveRecord;
import com.meng.lovespace.user.service.admin.AdminTimelineService;
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
@RequestMapping("/api/v1/admin/timeline/records")
public class AdminTimelineController {

    private final AdminTimelineService adminTimelineService;

    public AdminTimelineController(AdminTimelineService adminTimelineService) {
        this.adminTimelineService = adminTimelineService;
    }

    @GetMapping
    public ApiResponse<IPage<LoveRecord>> page(
            Authentication auth,
            @RequestParam(value = "coupleId", required = false) String coupleId,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminTimelineService.page(coupleId, userId, page, pageSize));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable("id") String id) {
        var admin = AdminAuthSupport.requireAdmin(auth);
        adminTimelineService.delete(admin.userId(), id);
        return ApiResponse.ok();
    }
}
