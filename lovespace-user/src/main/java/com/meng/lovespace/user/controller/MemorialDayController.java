package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.MemorialDayCreateRequest;
import com.meng.lovespace.user.dto.MemorialDayResponse;
import com.meng.lovespace.user.dto.MemorialDayUpdateRequest;
import com.meng.lovespace.user.dto.MemorialNextResponse;
import com.meng.lovespace.user.dto.MemorialUpcomingItemResponse;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.MemorialDayService;
import jakarta.validation.Valid;
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
 * 纪念日 CRUD 与倒计时查询；需登录（JWT 或分布式 Session）。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/memorial-days")
public class MemorialDayController {

    private final MemorialDayService memorialDayService;

    public MemorialDayController(MemorialDayService memorialDayService) {
        this.memorialDayService = memorialDayService;
    }

    @PostMapping
    public ApiResponse<MemorialDayResponse> create(Authentication auth, @Valid @RequestBody MemorialDayCreateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("memorial.api.create userId={} coupleId={}", p.userId(), req.coupleId());
        return ApiResponse.ok(memorialDayService.create(p.userId(), req));
    }

    @GetMapping
    public ApiResponse<List<MemorialDayResponse>> list(
            Authentication auth, @RequestParam("coupleId") @NotBlank(message = "coupleId is required") String coupleId) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("memorial.api.list userId={} coupleId={}", p.userId(), coupleId);
        return ApiResponse.ok(memorialDayService.listByCouple(p.userId(), coupleId));
    }

    @GetMapping("/{id}")
    public ApiResponse<MemorialDayResponse> get(Authentication auth, @PathVariable("id") String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("memorial.api.get userId={} id={}", p.userId(), id);
        return ApiResponse.ok(memorialDayService.getById(p.userId(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<MemorialDayResponse> update(
            Authentication auth, @PathVariable("id") String id, @Valid @RequestBody MemorialDayUpdateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("memorial.api.update userId={} id={}", p.userId(), id);
        return ApiResponse.ok(memorialDayService.update(p.userId(), id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable("id") String id) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("memorial.api.delete userId={} id={}", p.userId(), id);
        memorialDayService.delete(p.userId(), id);
        return ApiResponse.ok();
    }

    /**
     * 最近的下一个纪念日及剩余时间；{@code memorial} 为 null 表示暂无记录。
     *
     * @param useCache 默认 true；为 false 时跳过 Redis 直查 DB（调试用）。
     */
    @GetMapping("/next")
    public ApiResponse<MemorialNextResponse> next(
            Authentication auth,
            @RequestParam("coupleId") @NotBlank(message = "coupleId is required") String coupleId,
            @RequestParam(value = "useCache", defaultValue = "true") boolean useCache) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("memorial.api.next userId={} coupleId={}", p.userId(), coupleId);
        return ApiResponse.ok(memorialDayService.getNext(p.userId(), coupleId, useCache));
    }

    /**
     * 未来窗口内（默认 7 天，含今天）的纪念日，供轮询列表；数据来自 Redis 时仍按当前时间刷新毫秒字段。
     */
    @GetMapping("/upcoming")
    public ApiResponse<List<MemorialUpcomingItemResponse>> upcoming(
            Authentication auth,
            @RequestParam("coupleId") @NotBlank(message = "coupleId is required") String coupleId,
            @RequestParam(value = "useCache", defaultValue = "true") boolean useCache) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("memorial.api.upcoming userId={} coupleId={}", p.userId(), coupleId);
        return ApiResponse.ok(memorialDayService.listUpcoming(p.userId(), coupleId, useCache));
    }
}
