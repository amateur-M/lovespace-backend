package com.meng.lovespace.user.dto;

/**
 * 邀请发送成功后的返回。
 *
 * @param bindingId 待接受记录 ID，用于接受接口
 */
public record CoupleInviteResponse(String bindingId) {}
