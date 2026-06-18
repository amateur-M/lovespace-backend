package com.meng.lovespace.user.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端情侣绑定列表项。
 */
public record AdminCoupleListItem(
        String id,
        String userId1,
        String userId2,
        String user1Phone,
        String user2Phone,
        String user1Name,
        String user2Name,
        LocalDate startDate,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
