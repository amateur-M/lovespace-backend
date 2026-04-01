package com.meng.lovespace.user.dto;

import java.util.List;

/** 评论分页。 */
public record LoveRecordCommentPageResponse(long total, long page, long pageSize, List<LoveRecordCommentResponse> comments) {}
