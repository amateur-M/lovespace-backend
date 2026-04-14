package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发送情侣绑定邀请（按对方手机号查找用户）。
 *
 * @param inviteePhone 被邀请方手机号
 */
public record CoupleInviteRequest(
        @NotBlank(message = "inviteePhone is required") @Size(max = 20) String inviteePhone) {}
