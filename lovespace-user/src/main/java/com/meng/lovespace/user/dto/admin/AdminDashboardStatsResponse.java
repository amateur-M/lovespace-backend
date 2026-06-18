package com.meng.lovespace.user.dto.admin;

/**
 * 管理端仪表盘统计。
 *
 * @param userCount 用户数
 * @param coupleCount 情侣绑定数
 * @param timelineRecordCount 时间轴记录数
 * @param albumCount 相册数
 * @param photoCount 照片数
 * @param messageCount 私信数
 * @param planCount 计划数
 * @param memorialDayCount 纪念日数
 * @param loveQaDocumentCount 恋爱问答文档数
 * @param loveQaConversationCount 恋爱问答会话数
 */
public record AdminDashboardStatsResponse(
        long userCount,
        long coupleCount,
        long timelineRecordCount,
        long albumCount,
        long photoCount,
        long messageCount,
        long planCount,
        long memorialDayCount,
        long loveQaDocumentCount,
        long loveQaConversationCount) {}
