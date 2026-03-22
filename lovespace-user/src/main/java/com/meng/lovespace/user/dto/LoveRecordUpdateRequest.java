package com.meng.lovespace.user.dto;

import java.time.LocalDate;

/**
 * 更新恋爱记录；仅非 null 字段会写入。
 */
public record LoveRecordUpdateRequest(
        LocalDate recordDate,
        String content,
        String mood,
        String locationJson,
        Integer visibility,
        String tagsJson,
        String imagesJson) {}
