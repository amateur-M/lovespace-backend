package com.meng.lovespace.user.dto;

import java.util.List;
import java.util.Map;

/**
 * 恋爱记录情感分析报告。
 *
 * @param overallMood 整体倾向：positive / neutral / negative
 * @param moodScore 综合情绪分 0-100
 * @param emotionDistribution 各心情标签占比（百分比，键为 {@code happy} 等英文标签）
 * @param trendData 按日情绪趋势（仅包含有记录的日期）
 * @param insights AI 生成的共情与建议（通义千问）
 */
public record EmotionAnalysisReport(
        String overallMood,
        int moodScore,
        Map<String, Double> emotionDistribution,
        List<EmotionTrendPoint> trendData,
        String insights) {}
