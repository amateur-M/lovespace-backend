package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.config.AvatarUploadProperties;
import com.meng.lovespace.user.dto.AlbumCreateRequest;
import com.meng.lovespace.user.dto.AlbumResponse;
import com.meng.lovespace.user.dto.PhotoFavoriteRequest;
import com.meng.lovespace.user.dto.PhotoResponse;
import com.meng.lovespace.user.dto.PhotoUploadRequest;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.AlbumService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 情侣相册：相册与照片管理。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/albums")
public class AlbumController {

    private final AlbumService albumService;
    private final AvatarUploadProperties avatarUploadProperties;

    public AlbumController(AlbumService albumService, AvatarUploadProperties avatarUploadProperties) {
        this.albumService = albumService;
        this.avatarUploadProperties = avatarUploadProperties;
    }

    @PostMapping
    public ApiResponse<AlbumResponse> createAlbum(Authentication auth, @Valid @RequestBody AlbumCreateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(albumService.createAlbum(p.userId(), req));
    }

    @GetMapping
    public ApiResponse<List<AlbumResponse>> listAlbums(
            Authentication auth, @RequestParam("coupleId") @NotBlank(message = "coupleId is required") String coupleId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(albumService.listAlbums(p.userId(), coupleId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAlbum(Authentication auth, @PathVariable("id") String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        albumService.deleteAlbum(p.userId(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/photos")
    public ApiResponse<PhotoResponse> uploadPhoto(
            Authentication auth,
            @PathVariable("id") String id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "thumbnailUrl", required = false) String thumbnailUrl,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "locationJson", required = false) String locationJson,
            @RequestParam(value = "takenDate", required = false) LocalDate takenDate,
            @RequestParam(value = "tagsJson", required = false) String tagsJson) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        validateImage(file);
        PhotoUploadRequest req = new PhotoUploadRequest(thumbnailUrl, description, locationJson, takenDate, tagsJson);
        return ApiResponse.ok(albumService.uploadPhoto(p.userId(), id, file, req));
    }

    @GetMapping("/{id}/photos")
    public ApiResponse<List<PhotoResponse>> listPhotos(Authentication auth, @PathVariable("id") String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(albumService.listPhotos(p.userId(), id));
    }

    @DeleteMapping("/{albumId}/photos/{photoId}")
    public ApiResponse<Void> deletePhoto(
            Authentication auth,
            @PathVariable("albumId") String albumId,
            @PathVariable("photoId") String photoId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        albumService.deletePhoto(p.userId(), albumId, photoId);
        return ApiResponse.ok();
    }

    @PutMapping("/{albumId}/photos/{photoId}/favorite")
    public ApiResponse<Void> setFavorite(
            Authentication auth,
            @PathVariable("albumId") String albumId,
            @PathVariable("photoId") String photoId,
            @Valid @RequestBody PhotoFavoriteRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        albumService.setFavorite(p.userId(), albumId, photoId, req.isFavorite());
        return ApiResponse.ok();
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.meng.lovespace.user.exception.AlbumBusinessException(40060, "file is required");
        }
        String ext = extensionOf(file.getOriginalFilename());
        List<String> allowed = avatarUploadProperties.allowedExtensions();
        if (allowed == null || allowed.isEmpty()) {
            allowed = List.of("jpg", "jpeg", "png", "webp");
        }
        List<String> lower = allowed.stream().map(String::toLowerCase).toList();
        if (!lower.contains(ext)) {
            throw new com.meng.lovespace.user.exception.AlbumBusinessException(
                    40062, "invalid image type, allowed: " + String.join(",", allowed));
        }
    }

    private static String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
