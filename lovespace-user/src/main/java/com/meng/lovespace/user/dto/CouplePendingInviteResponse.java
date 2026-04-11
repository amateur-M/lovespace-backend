package com.meng.lovespace.user.dto;

import java.time.LocalDateTime;

/**
 * 待处理的情侣邀请（被邀请方视角）。
 *
 * @param bindingId 绑定记录 ID，接受邀请时原样提交
 * @param inviter 邀请方用户资料
 * @param invitedAt 邀请发出时间
 */
public record CouplePendingInviteResponse(String bindingId, UserResponse inviter, LocalDateTime invitedAt) {}
