package com.meng.lovespace.user.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.admin.AdminAuthSupport;
import com.meng.lovespace.user.entity.LoveQaConversation;
import com.meng.lovespace.user.entity.LoveQaDocument;
import com.meng.lovespace.user.service.admin.AdminLoveQaService;
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
@RequestMapping("/api/v1/admin/love-qa")
public class AdminLoveQaController {

    private final AdminLoveQaService adminLoveQaService;

    public AdminLoveQaController(AdminLoveQaService adminLoveQaService) {
        this.adminLoveQaService = adminLoveQaService;
    }

    @GetMapping("/documents")
    public ApiResponse<IPage<LoveQaDocument>> pageDocuments(
            Authentication auth,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "coupleId", required = false) String coupleId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminLoveQaService.pageDocuments(scope, coupleId, page, pageSize));
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> deleteDocument(
            Authentication auth, @PathVariable("id") String id) {
        var admin = AdminAuthSupport.requireAdmin(auth);
        adminLoveQaService.deleteDocument(admin.userId(), id);
        return ApiResponse.ok();
    }

    @GetMapping("/conversations")
    public ApiResponse<IPage<LoveQaConversation>> pageConversations(
            Authentication auth,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminLoveQaService.pageConversations(page, pageSize));
    }
}
