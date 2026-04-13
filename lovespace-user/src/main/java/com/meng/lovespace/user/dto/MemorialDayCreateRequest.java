package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 创建纪念日请求。
 */
public record MemorialDayCreateRequest(
        @NotBlank(message = "coupleId is required") String coupleId,
        @NotBlank(message = "name is required") @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull(message = "memorialDate is required") LocalDate memorialDate) {}
