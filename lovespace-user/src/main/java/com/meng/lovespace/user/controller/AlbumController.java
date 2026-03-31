package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.config.AvatarUploadProperties;
import com.meng.lovespace.user.dto.AlbumCreateRequest;
import com.meng.lovespace.user.dto.AlbumPhotoPageResponse;
import com.meng.lovespace.user.dto.AlbumResponse;
import com.meng.lovespace.user.dto.PhotoFavoriteRequest;
import com.meng.lovespace.user.dto.PhotoRegisterFromUrlRequest;
import com.meng.lovespace.user.dto.PhotoResponse;
import com.meng.lovespace.user.dto.PhotoUploadRequest;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.AlbumService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 * 情侣相册 HTTP 接口：相册 CRUD、照片上传与列表、收藏。
 *
 * <p>上传字段名 {@code file}；类型校验见 {@link #validateImage}，大小由 {@code spring.servlet.multipart} 限制，不在此重复校验。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/albums")
public class AlbumController {

    private final AlbumService albumService;
    private final AvatarUploadProperties avatarUploadProperties;

    /** @param albumService 相册领域服务；avatarUploadProperties 用于相册图扩展名白名单（与头像配置共用） */
    public AlbumController(AlbumService albumService, AvatarUploadProperties avatarUploadProperties) {
        this.albumService = albumService;
        this.avatarUploadProperties = avatarUploadProperties;
    }

    @PostMapping
    public ApiResponse<AlbumResponse> createAlbum(Authentication auth, @Valid @RequestBody AlbumCreateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("album.api.create userId={} coupleId={}", p.userId(), req.coupleId());
        return ApiResponse.ok(albumService.createAlbum(p.userId(), req));
    }

    @GetMapping
    public ApiResponse<List<AlbumResponse>> listAlbums(
            Authentication auth, @RequestParam("coupleId") @NotBlank(message = "coupleId is required") String coupleId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("album.api.list userId={} coupleId={}", p.userId(), coupleId);
        return ApiResponse.ok(albumService.listAlbums(p.userId(), coupleId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAlbum(Authentication auth, @PathVariable("id") String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("album.api.delete userId={} albumId={}", p.userId(), id);
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
        log.info(
                "album.api.upload start userId={} albumId={} originalName={} size={} contentType={}",
                p.userId(),
                id,
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType());
        PhotoUploadRequest req = new PhotoUploadRequest(thumbnailUrl, description, locationJson, takenDate, tagsJson);
        return ApiResponse.ok(albumService.uploadPhoto(p.userId(), id, file, req));
    }

    /**
     * 分片合并得到 {@code imageUrl} 后登记照片（与 {@link #uploadPhoto} 二选一）。
     */
    @PostMapping("/{albumId}/photos/from-url")
    public ApiResponse<PhotoResponse> registerPhotoFromUrl(
            Authentication auth,
            @PathVariable("albumId") String albumId,
            @Valid @RequestBody PhotoRegisterFromUrlRequest body) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        PhotoUploadRequest meta =
                new PhotoUploadRequest(
                        body.thumbnailUrl(),
                        body.description(),
                        body.locationJson(),
                        body.takenDate(),
                        body.tagsJson());
        return ApiResponse.ok(albumService.registerPhotoFromUploadedUrl(p.userId(), albumId, body.imageUrl(), meta));
    }

    @GetMapping("/{id}/photos")
    public ApiResponse<AlbumPhotoPageResponse> listPhotos(
            Authentication auth,
            @PathVariable("id") String id,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) long page,
            @RequestParam(value = "pageSize", defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug(
                "album.api.photos.list userId={} albumId={} page={} pageSize={}",
                p.userId(),
                id,
                page,
                pageSize);
        return ApiResponse.ok(albumService.pagePhotos(p.userId(), id, page, pageSize));
    }

    @DeleteMapping("/{albumId}/photos/{photoId}")
    public ApiResponse<Void> deletePhoto(
            Authentication auth,
            @PathVariable("albumId") String albumId,
            @PathVariable("photoId") String photoId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("album.api.photo.delete userId={} albumId={} photoId={}", p.userId(), albumId, photoId);
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
        log.debug(
                "album.api.favorite userId={} albumId={} photoId={} favorite={}",
                p.userId(),
                albumId,
                photoId,
                req.isFavorite());
        albumService.setFavorite(p.userId(), albumId, photoId, req.isFavorite());
        return ApiResponse.ok();
    }

    /** 校验非空与扩展名；大小由 Spring multipart 配置约束。 */
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("album.upload rejected reason=empty");
            throw new com.meng.lovespace.user.exception.AlbumBusinessException(40060, "file is required");
        }
        String ext = extensionOf(file.getOriginalFilename());
        List<String> allowed = avatarUploadProperties.allowedExtensions();
        if (allowed == null || allowed.isEmpty()) {
            allowed = List.of("jpg", "jpeg", "png", "webp");
        }
        List<String> lower = allowed.stream().map(String::toLowerCase).toList();
        if (!lower.contains(ext)) {
            log.warn(
                    "album.upload rejected reason=bad_ext ext={} allowed={} name={}",
                    ext,
                    allowed,
                    file.getOriginalFilename());
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
