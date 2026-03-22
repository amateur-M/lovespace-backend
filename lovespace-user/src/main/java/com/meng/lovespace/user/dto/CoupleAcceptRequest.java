package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * 接受情侣绑定邀请。
 *
 * @param bindingId 邀请对应的绑定记录 ID（邀请接口返回）
 * @param startDate 恋爱开始日；为空则默认使用服务端当日（业务时区）
 */
public record CoupleAcceptRequest(
        @NotBlank(message = "bindingId is required") String bindingId, LocalDate startDate) {}
