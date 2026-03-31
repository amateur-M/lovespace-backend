package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * 创建恋爱时间轴记录。
 */
public record LoveRecordCreateRequest(
        @NotBlank(message = "coupleId is required") String coupleId,
        @NotNull(message = "recordDate is required") LocalDate recordDate,
        @NotBlank(message = "content is required") String content,
        @NotBlank(message = "mood is required")
                @Pattern(
                        regexp = "happy|sad|excited|calm|loved|missed",
                        message = "invalid mood")
                String mood,
        String locationJson,
        @NotNull(message = "visibility is required") @Min(1) @Max(2) Integer visibility,
        String tagsJson,
        /** 图片与视频 URL 的 JSON 数组字符串，写入 {@code love_records.images_json} */
        String imagesJson) {}
