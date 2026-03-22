package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 发送情侣绑定邀请。
 *
 * @param inviteeUserId 被邀请用户 ID
 */
public record CoupleInviteRequest(@NotBlank(message = "inviteeUserId is required") String inviteeUserId) {}
