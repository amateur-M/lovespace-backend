package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 更新照片元数据（整单替换描述、位置、拍摄日、标签 JSON）。
 *
 * <p>各字段均可为 {@code null}，表示将对应列置空；拍摄日不可晚于业务当日。
 */
public record PhotoUpdateRequest(
        @Size(max = 500, message = "description too long") String description,
        String locationJson,
        LocalDate takenDate,
        String tagsJson) {}
