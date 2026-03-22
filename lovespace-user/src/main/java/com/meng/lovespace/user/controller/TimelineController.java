package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.LoveRecordCreateRequest;
import com.meng.lovespace.user.dto.LoveRecordPageResponse;
import com.meng.lovespace.user.dto.LoveRecordResponse;
import com.meng.lovespace.user.dto.LoveRecordUpdateRequest;
import com.meng.lovespace.user.config.AvatarUploadProperties;
import com.meng.lovespace.user.oss.AvatarStorageService;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.LoveRecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
 * 恋爱时间轴：记录的增删改查与回忆推送。
 *
 * <p>所有接口需登录；{@code coupleId} 须为当前用户处于交往/冻结状态的情侣绑定 ID。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/timeline")
public class TimelineController {

    private final LoveRecordService loveRecordService;
    private final AvatarStorageService avatarStorageService;
    private final AvatarUploadProperties avatarUploadProperties;

    public TimelineController(
            LoveRecordService loveRecordService,
            AvatarStorageService avatarStorageService,
            AvatarUploadProperties avatarUploadProperties) {
        this.loveRecordService = loveRecordService;
        this.avatarStorageService = avatarStorageService;
        this.avatarUploadProperties = avatarUploadProperties;
    }

    /**
     * 上传时间轴配图（与头像相同的类型/大小限制），返回可写入 {@code images_json} 的 URL。
     */
    @PostMapping("/upload")
    public ApiResponse<String> uploadTimelineImage(
            Authentication auth, @RequestPart("file") MultipartFile file) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(40010, "file is required");
        }
        if (file.getSize() > avatarUploadProperties.maxSizeBytes()) {
            return ApiResponse.error(
                    40011,
                    "image too large, max "
                            + avatarUploadProperties.maxSizeBytes() / 1024 / 1024
                            + "MB");
        }
        String ext = extensionOf(file.getOriginalFilename());
        java.util.List<String> allowed = avatarUploadProperties.allowedExtensions();
        if (allowed == null || allowed.isEmpty()) {
            allowed = java.util.List.of("jpg", "jpeg", "png", "webp");
        }
        if (!allowed.stream().map(String::toLowerCase).toList().contains(ext)) {
            return ApiResponse.error(40012, "invalid image type, allowed: " + String.join(",", allowed));
        }
        String url = avatarStorageService.uploadTimelineImage(p.userId(), file);
        log.info("timeline.upload userId={} url={}", p.userId(), url);
        return ApiResponse.ok(url);
    }

    @PostMapping("/records")
    public ApiResponse<LoveRecordResponse> createRecord(
            Authentication auth, @Valid @RequestBody LoveRecordCreateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("timeline.create userId={} coupleId={}", p.userId(), req.coupleId());
        return ApiResponse.ok(loveRecordService.create(p.userId(), req));
    }

    @GetMapping("/records")
    public ApiResponse<LoveRecordPageResponse> listRecords(
            Authentication auth,
            @RequestParam("coupleId") @NotBlank(message = "coupleId is required") String coupleId,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) long page,
            @RequestParam(value = "pageSize", defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(loveRecordService.pageRecords(p.userId(), coupleId, page, pageSize));
    }

    @GetMapping("/records/{id}")
    public ApiResponse<LoveRecordResponse> getRecord(Authentication auth, @PathVariable String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(loveRecordService.getDetail(p.userId(), id));
    }

    @PutMapping("/records/{id}")
    public ApiResponse<Void> updateRecord(
            Authentication auth, @PathVariable("id") String id, @RequestBody LoveRecordUpdateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        loveRecordService.update(p.userId(), id, req);
        return ApiResponse.ok();
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> deleteRecord(Authentication auth, @PathVariable("id") String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        loveRecordService.delete(p.userId(), id);
        return ApiResponse.ok();
    }

    /**
     * 回忆推送：优先返回历年「同月同日」记录，不足时补充更多历史记录（最多 30 条）。
     */
    @GetMapping("/memories")
    public ApiResponse<List<LoveRecordResponse>> memories(
            Authentication auth,
            @RequestParam("coupleId") @NotBlank(message = "coupleId is required") String coupleId,
            @RequestParam(value = "limit", defaultValue = "10") @Min(1) @Max(30) int limit) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(loveRecordService.memories(p.userId(), coupleId, limit));
    }

    private static String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
