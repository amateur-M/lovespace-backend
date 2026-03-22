package com.meng.lovespace.user.dto;

import java.time.LocalDate;

/**
 * 当前用户的情侣绑定信息视图。
 *
 * @param bindingId 绑定记录 ID
 * @param startDate 恋爱开始日
 * @param relationshipDays 恋爱天数（由开始日自动计算）
 * @param status 状态：1 交往 2 冻结
 * @param partner 对方用户资料
 */
public record CoupleInfoResponse(
        String bindingId,
        LocalDate startDate,
        int relationshipDays,
        int status,
        UserResponse partner) {}
