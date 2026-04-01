package com.meng.lovespace.user.dto;

/** 点赞切换后的计数与当前用户状态。 */
public record LoveRecordLikeStateResponse(int likeCount, boolean likedByMe) {}
