package com.meng.lovespace.user.dto;

import java.time.LocalDateTime;

/** 单条评论视图（含作者展示名）。 */
public record LoveRecordCommentResponse(
        long id,
        String recordId,
        String userId,
        String authorUsername,
        String content,
        LocalDateTime createdAt) {}
