package com.meng.lovespace.user.dto;

import java.util.List;

/** 分页查询恋爱记录。 */
public record LoveRecordPageResponse(long total, long page, long pageSize, List<LoveRecordResponse> records) {}
