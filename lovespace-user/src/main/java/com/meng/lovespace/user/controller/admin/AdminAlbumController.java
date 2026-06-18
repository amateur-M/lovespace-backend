package com.meng.lovespace.user.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.admin.AdminAuthSupport;
import com.meng.lovespace.user.entity.Album;
import com.meng.lovespace.user.entity.Photo;
import com.meng.lovespace.user.service.admin.AdminAlbumService;
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
@RequestMapping("/api/v1/admin/albums")
public class AdminAlbumController {

    private final AdminAlbumService adminAlbumService;

    public AdminAlbumController(AdminAlbumService adminAlbumService) {
        this.adminAlbumService = adminAlbumService;
    }

    @GetMapping
    public ApiResponse<IPage<Album>> pageAlbums(
            Authentication auth,
            @RequestParam(value = "coupleId", required = false) String coupleId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminAlbumService.pageAlbums(coupleId, page, pageSize));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAlbum(Authentication auth, @PathVariable("id") String id) {
        var admin = AdminAuthSupport.requireAdmin(auth);
        adminAlbumService.deleteAlbum(admin.userId(), id);
        return ApiResponse.ok();
    }

    @GetMapping("/photos")
    public ApiResponse<IPage<Photo>> pagePhotos(
            Authentication auth,
            @RequestParam(value = "albumId", required = false) String albumId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        AdminAuthSupport.requireAdmin(auth);
        return ApiResponse.ok(adminAlbumService.pagePhotos(albumId, page, pageSize));
    }

    @DeleteMapping("/photos/{id}")
    public ApiResponse<Void> deletePhoto(Authentication auth, @PathVariable("id") String id) {
        var admin = AdminAuthSupport.requireAdmin(auth);
        adminAlbumService.deletePhoto(admin.userId(), id);
        return ApiResponse.ok();
    }
}
