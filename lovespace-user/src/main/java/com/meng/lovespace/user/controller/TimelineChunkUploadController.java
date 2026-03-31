package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.TimelineUploadInitRequest;
import com.meng.lovespace.user.dto.TimelineUploadInitResponse;
import com.meng.lovespace.user.dto.TimelineUploadStatusResponse;
import com.meng.lovespace.user.exception.TimelineBusinessException;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.TimelineChunkUploadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.io.IOException;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 时间轴媒体分片上传：初始化、按片写入、查询进度、合并完成、取消。
 *
 * <p>需登录；会话绑定创建者 userId。分片请求体为原始二进制，{@code Content-Length} 必须与该片理论长度一致。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/timeline/uploads")
public class TimelineChunkUploadController {

    private final TimelineChunkUploadService chunkUploadService;

    /** @param chunkUploadService 分片上传领域服务 */
    public TimelineChunkUploadController(TimelineChunkUploadService chunkUploadService) {
        this.chunkUploadService = chunkUploadService;
    }

    /** 创建上传会话。 */
    @PostMapping("/init")
    public ApiResponse<TimelineUploadInitResponse> init(
            Authentication auth, @Valid @RequestBody TimelineUploadInitRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(chunkUploadService.initUpload(p.userId(), req));
    }

    /**
     * 上传一个分片。
     *
     * @param chunkIndex 从 0 开始
     */
    @PutMapping("/{uploadId}/chunks/{chunkIndex}")
    public ApiResponse<Void> putChunk(
            Authentication auth,
            @PathVariable("uploadId") String uploadId,
            @PathVariable("chunkIndex") @Min(0) int chunkIndex,
            HttpServletRequest request) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        long cl = request.getContentLengthLong();
        try {
            chunkUploadService.writeChunk(p.userId(), uploadId, chunkIndex, cl, request.getInputStream());
        } catch (IOException e) {
            log.warn(
                    "timeline.chunk.put io userId={} uploadId={} index={} msg={}",
                    p.userId(),
                    uploadId,
                    chunkIndex,
                    e.getMessage());
            throw new TimelineBusinessException(50010, "chunk upload io failed");
        }
        return ApiResponse.ok();
    }

    /** 查询已上传分片下标（断点续传）。 */
    @GetMapping("/{uploadId}/status")
    public ApiResponse<TimelineUploadStatusResponse> status(
            Authentication auth, @PathVariable("uploadId") String uploadId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(chunkUploadService.status(p.userId(), uploadId));
    }

    /** 合并分片并发布到存储，返回与直传相同的访问 URL。 */
    @PostMapping("/{uploadId}/complete")
    public ApiResponse<String> complete(Authentication auth, @PathVariable("uploadId") String uploadId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        String url = chunkUploadService.complete(p.userId(), uploadId);
        log.info("timeline.chunk.api.complete userId={} uploadId={}", p.userId(), uploadId);
        return ApiResponse.ok(url);
    }

    /** 取消会话并删除暂存分片。 */
    @DeleteMapping("/{uploadId}")
    public ApiResponse<Void> abort(Authentication auth, @PathVariable("uploadId") String uploadId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        chunkUploadService.abort(p.userId(), uploadId);
        return ApiResponse.ok();
    }
}
