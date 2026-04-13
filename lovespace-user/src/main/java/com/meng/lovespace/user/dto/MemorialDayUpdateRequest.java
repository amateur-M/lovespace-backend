package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 更新纪念日请求（整单替换可空字段以外的内容）。
 */
public record MemorialDayUpdateRequest(
        @NotBlank(message = "name is required") @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull(message = "memorialDate is required") LocalDate memorialDate) {}
