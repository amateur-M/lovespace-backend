package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.MediaChunkInitRequest;
import com.meng.lovespace.user.dto.MediaChunkInitResponse;
import com.meng.lovespace.user.dto.MediaChunkStatusResponse;
import com.meng.lovespace.user.exception.MediaChunkBusinessException;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.ChunkedMediaUploadService;
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
 * 时间轴与相册共用的分片上传 API。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/media/uploads")
public class MediaChunkUploadController {

    private final ChunkedMediaUploadService chunkedMediaUploadService;

    public MediaChunkUploadController(ChunkedMediaUploadService chunkedMediaUploadService) {
        this.chunkedMediaUploadService = chunkedMediaUploadService;
    }

    @PostMapping("/init")
    public ApiResponse<MediaChunkInitResponse> init(
            Authentication auth, @Valid @RequestBody MediaChunkInitRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(chunkedMediaUploadService.initUpload(p.userId(), req));
    }

    @PutMapping("/{uploadId}/chunks/{chunkIndex}")
    public ApiResponse<Void> putChunk(
            Authentication auth,
            @PathVariable("uploadId") String uploadId,
            @PathVariable("chunkIndex") @Min(0) int chunkIndex,
            HttpServletRequest request) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        long cl = request.getContentLengthLong();
        try {
            chunkedMediaUploadService.writeChunk(p.userId(), uploadId, chunkIndex, cl, request.getInputStream());
        } catch (IOException e) {
            log.warn(
                    "media.chunk.put io userId={} uploadId={} index={} msg={}",
                    p.userId(),
                    uploadId,
                    chunkIndex,
                    e.getMessage());
            throw new MediaChunkBusinessException(50010, "chunk upload io failed");
        }
        return ApiResponse.ok();
    }

    @GetMapping("/{uploadId}/status")
    public ApiResponse<MediaChunkStatusResponse> status(
            Authentication auth, @PathVariable("uploadId") String uploadId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(chunkedMediaUploadService.status(p.userId(), uploadId));
    }

    @PostMapping("/{uploadId}/complete")
    public ApiResponse<String> complete(Authentication auth, @PathVariable("uploadId") String uploadId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        String url = chunkedMediaUploadService.complete(p.userId(), uploadId);
        log.info("media.chunk.api.complete userId={} uploadId={}", p.userId(), uploadId);
        return ApiResponse.ok(url);
    }

    @DeleteMapping("/{uploadId}")
    public ApiResponse<Void> abort(Authentication auth, @PathVariable("uploadId") String uploadId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        chunkedMediaUploadService.abort(p.userId(), uploadId);
        return ApiResponse.ok();
    }
}
