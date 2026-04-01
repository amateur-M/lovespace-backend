package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.LoveRecordCommentCreateRequest;
import com.meng.lovespace.user.dto.LoveRecordCommentPageResponse;
import com.meng.lovespace.user.dto.LoveRecordCommentResponse;
import com.meng.lovespace.user.dto.LoveRecordCreateRequest;
import com.meng.lovespace.user.dto.LoveRecordLikeStateResponse;
import com.meng.lovespace.user.dto.LoveRecordPageResponse;
import com.meng.lovespace.user.dto.LoveRecordResponse;
import com.meng.lovespace.user.dto.LoveRecordUpdateRequest;
import com.meng.lovespace.user.config.TimelineUploadProperties;
import com.meng.lovespace.user.oss.AvatarStorageService;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.LoveRecordService;
import com.meng.lovespace.user.service.LoveRecordSocialService;
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
    private final LoveRecordSocialService loveRecordSocialService;
    private final AvatarStorageService avatarStorageService;
    private final TimelineUploadProperties timelineUploadProperties;

    /**
     * @param loveRecordService 时间轴领域服务
     * @param loveRecordSocialService 点赞与评论
     * @param avatarStorageService 对象存储（时间轴媒体与头像共用策略）
     * @param timelineUploadProperties 时间轴图片/视频分档大小与扩展名校验
     */
    public TimelineController(
            LoveRecordService loveRecordService,
            LoveRecordSocialService loveRecordSocialService,
            AvatarStorageService avatarStorageService,
            TimelineUploadProperties timelineUploadProperties) {
        this.loveRecordService = loveRecordService;
        this.loveRecordSocialService = loveRecordSocialService;
        this.avatarStorageService = avatarStorageService;
        this.timelineUploadProperties = timelineUploadProperties;
    }

    /**
     * 上传时间轴配图或短视频，返回可写入 {@code images_json} 的 URL（与图片共用 JSON 数组字段）。
     */
    @PostMapping("/upload")
    public ApiResponse<String> uploadTimelineMedia(
            Authentication auth, @RequestPart("file") MultipartFile file) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        if (file == null || file.isEmpty()) {
            log.warn("timeline.upload rejected userId={} reason=empty", p.userId());
            return ApiResponse.error(40010, "file is required");
        }
        String ext = extensionOf(file.getOriginalFilename());
        List<String> imgExt = timelineUploadProperties.imageExtensions();
        List<String> vidExt = timelineUploadProperties.videoExtensions();
        if (imgExt == null || imgExt.isEmpty()) {
            imgExt = List.of("jpg", "jpeg", "png", "webp");
        }
        if (vidExt == null || vidExt.isEmpty()) {
            vidExt = List.of("mp4", "webm", "mov");
        }
        List<String> imgLower = imgExt.stream().map(String::toLowerCase).toList();
        List<String> vidLower = vidExt.stream().map(String::toLowerCase).toList();

        boolean isImage = imgLower.contains(ext);
        boolean isVideo = vidLower.contains(ext);
        if (!isImage && !isVideo) {
            log.warn(
                    "timeline.upload rejected userId={} reason=bad_ext ext={} images={} videos={}",
                    p.userId(),
                    ext,
                    imgLower,
                    vidLower);
            return ApiResponse.error(
                    40012,
                    "invalid media type, images: "
                            + String.join(",", imgLower)
                            + "; videos: "
                            + String.join(",", vidLower));
        }
        long maxBytes = isVideo ? timelineUploadProperties.videoMaxSizeBytes() : timelineUploadProperties.imageMaxSizeBytes();
        if (file.getSize() > maxBytes) {
            log.warn(
                    "timeline.upload rejected userId={} reason=too_large kind={} size={} maxBytes={}",
                    p.userId(),
                    isVideo ? "video" : "image",
                    file.getSize(),
                    maxBytes);
            String kind = isVideo ? "video" : "image";
            return ApiResponse.error(40011, kind + " too large, max " + maxBytes / 1024 / 1024 + "MB");
        }
        String url = avatarStorageService.uploadTimelineImage(p.userId(), file);
        log.info("timeline.upload userId={} kind={} url={}", p.userId(), isVideo ? "video" : "image", url);
        return ApiResponse.ok(url);
    }

    /**
     * 创建恋爱记录。
     *
     * @param auth 当前用户（作为作者）
     * @param req 请求体
     */
    @PostMapping("/records")
    public ApiResponse<LoveRecordResponse> createRecord(
            Authentication auth, @Valid @RequestBody LoveRecordCreateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("timeline.create userId={} coupleId={}", p.userId(), req.coupleId());
        return ApiResponse.ok(loveRecordService.create(p.userId(), req));
    }

    /**
     * 分页查询记录；可选 {@code startDate}/{@code endDate} 筛选 {@code record_date}。
     */
    @GetMapping("/records")
    public ApiResponse<LoveRecordPageResponse> listRecords(
            Authentication auth,
            @RequestParam("coupleId") @NotBlank(message = "coupleId is required") String coupleId,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) long page,
            @RequestParam(value = "pageSize", defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug(
                "timeline.list userId={} coupleId={} page={} pageSize={} startDate={} endDate={}",
                p.userId(),
                coupleId,
                page,
                pageSize,
                startDate,
                endDate);
        return ApiResponse.ok(
                loveRecordService.pageRecords(p.userId(), coupleId, page, pageSize, startDate, endDate)
        );
    }

    /** 查询单条记录详情。 */
    @GetMapping("/records/{id}")
    public ApiResponse<LoveRecordResponse> getRecord(Authentication auth, @PathVariable("id") String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("timeline.get userId={} recordId={}", p.userId(), id);
        return ApiResponse.ok(loveRecordService.getDetail(p.userId(), id));
    }

    /** 更新记录（仅作者）。 */
    @PutMapping("/records/{id}")
    public ApiResponse<Void> updateRecord(
            Authentication auth, @PathVariable("id") String id, @RequestBody LoveRecordUpdateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("timeline.update userId={} recordId={}", p.userId(), id);
        loveRecordService.update(p.userId(), id, req);
        return ApiResponse.ok();
    }

    /** 删除记录（仅作者）。 */
    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> deleteRecord(Authentication auth, @PathVariable("id") String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("timeline.delete userId={} recordId={}", p.userId(), id);
        loveRecordService.delete(p.userId(), id);
        return ApiResponse.ok();
    }

    /** 切换点赞（已赞则取消）。 */
    @PostMapping("/records/{id}/like")
    public ApiResponse<LoveRecordLikeStateResponse> toggleLike(Authentication auth, @PathVariable("id") String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("timeline.like userId={} recordId={}", p.userId(), id);
        return ApiResponse.ok(loveRecordSocialService.toggleLike(p.userId(), id));
    }

    /** 分页查询评论（时间正序）。 */
    @GetMapping("/records/{id}/comments")
    public ApiResponse<LoveRecordCommentPageResponse> listComments(
            Authentication auth,
            @PathVariable("id") String id,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) long page,
            @RequestParam(value = "pageSize", defaultValue = "20") @Min(1) @Max(50) long pageSize) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("timeline.comments userId={} recordId={} page={}", p.userId(), id, page);
        return ApiResponse.ok(loveRecordSocialService.pageComments(p.userId(), id, page, pageSize));
    }

    /** 发表评论。 */
    @PostMapping("/records/{id}/comments")
    public ApiResponse<LoveRecordCommentResponse> addComment(
            Authentication auth,
            @PathVariable("id") String id,
            @Valid @RequestBody LoveRecordCommentCreateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("timeline.comment userId={} recordId={}", p.userId(), id);
        return ApiResponse.ok(loveRecordSocialService.addComment(p.userId(), id, req));
    }

    /** 删除自己的评论。 */
    @DeleteMapping("/records/{id}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            Authentication auth, @PathVariable("id") String id, @PathVariable("commentId") long commentId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("timeline.commentDelete userId={} recordId={} commentId={}", p.userId(), id, commentId);
        loveRecordSocialService.deleteComment(p.userId(), id, commentId);
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
        log.debug("timeline.memories userId={} coupleId={} limit={}", p.userId(), coupleId, limit);
        return ApiResponse.ok(loveRecordService.memories(p.userId(), coupleId, limit));
    }

    /** 从原始文件名解析小写扩展名（无点则空串）。 */
    private static String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
