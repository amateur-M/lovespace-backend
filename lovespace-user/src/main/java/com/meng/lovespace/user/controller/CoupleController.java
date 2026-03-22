package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.CoupleAcceptRequest;
import com.meng.lovespace.user.dto.CoupleInfoResponse;
import com.meng.lovespace.user.dto.CoupleInviteRequest;
import com.meng.lovespace.user.dto.CoupleInviteResponse;
import com.meng.lovespace.user.dto.CoupleUpdateStartDateRequest;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.CoupleBindingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 情侣绑定：邀请、接受、查询、更新恋爱开始日、解除关系。
 *
 * <p>需登录 JWT；业务错误通过 {@link com.meng.lovespace.user.exception.CoupleBindingBusinessException} 转为 {@link ApiResponse}。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/couple")
public class CoupleController {

    private final CoupleBindingService coupleBindingService;

    public CoupleController(CoupleBindingService coupleBindingService) {
        this.coupleBindingService = coupleBindingService;
    }

    /**
     * 向指定用户发送绑定邀请。
     *
     * @param auth 当前用户（邀请方）
     * @param req 被邀请用户 ID
     */
    @PostMapping("/invite")
    public ApiResponse<CoupleInviteResponse> invite(Authentication auth, @Valid @RequestBody CoupleInviteRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("couple.invite inviterId={} inviteeId={}", p.userId(), req.inviteeUserId());
        CoupleInviteResponse data = coupleBindingService.invite(p.userId(), req.inviteeUserId());
        return ApiResponse.ok(data);
    }

    /**
     * 接受邀请，成为情侣；可选指定恋爱开始日，默认当天（服务端业务时区）。
     */
    @PostMapping("/accept")
    public ApiResponse<CoupleInfoResponse> accept(Authentication auth, @Valid @RequestBody CoupleAcceptRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("couple.accept userId={} bindingId={}", p.userId(), req.bindingId());
        CoupleInfoResponse data = coupleBindingService.accept(p.userId(), req);
        return ApiResponse.ok(data);
    }

    /** 获取当前进行中的情侣信息（交往或冻结）。 */
    @GetMapping("/info")
    public ApiResponse<CoupleInfoResponse> info(Authentication auth) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        return coupleBindingService
                .getCoupleInfo(p.userId())
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.error(40442, "no active couple binding"));
    }

    /** 更新恋爱开始日，并自动重算恋爱天数。 */
    @PutMapping("/start-date")
    public ApiResponse<Void> updateStartDate(
            Authentication auth, @Valid @RequestBody CoupleUpdateStartDateRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("couple.start-date userId={} startDate={}", p.userId(), req.startDate());
        coupleBindingService.updateStartDate(p.userId(), req.startDate());
        return ApiResponse.ok();
    }

    /** 解除当前情侣关系（标记为 separated）。 */
    @PostMapping("/separate")
    public ApiResponse<Void> separate(Authentication auth) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info("couple.separate userId={}", p.userId());
        coupleBindingService.separate(p.userId());
        return ApiResponse.ok();
    }
}
