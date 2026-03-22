package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.LoveRecordCreateRequest;
import com.meng.lovespace.user.dto.LoveRecordPageResponse;
import com.meng.lovespace.user.dto.LoveRecordResponse;
import com.meng.lovespace.user.dto.LoveRecordUpdateRequest;
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
import org.springframework.web.bind.annotation.RestController;

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

    public TimelineController(LoveRecordService loveRecordService) {
        this.loveRecordService = loveRecordService;
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
            @RequestParam @NotBlank(message = "coupleId is required") String coupleId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize) {
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
            Authentication auth, @PathVariable String id, @RequestBody LoveRecordUpdateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        loveRecordService.update(p.userId(), id, req);
        return ApiResponse.ok();
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> deleteRecord(Authentication auth, @PathVariable String id) {
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
            @RequestParam @NotBlank(message = "coupleId is required") String coupleId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int limit) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return ApiResponse.ok(loveRecordService.memories(p.userId(), coupleId, limit));
    }
}
