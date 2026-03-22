package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 更新当前情侣的恋爱开始日。
 *
 * @param startDate 新的开始日，不能晚于今天（业务时区）
 */
public record CoupleUpdateStartDateRequest(@NotNull(message = "startDate is required") LocalDate startDate) {}
