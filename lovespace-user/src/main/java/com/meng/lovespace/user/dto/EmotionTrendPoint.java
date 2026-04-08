package com.meng.lovespace.user.dto;

/**
 * 单日情绪趋势点：该日综合得分与主导心情标签。
 *
 * @param date 记录日期 yyyy-MM-dd
 * @param moodScore 当日综合情绪分 0-100
 * @param dominantMood 当日出现最多的心情标签（{@link com.meng.lovespace.user.timeline.LoveMood}）
 */
public record EmotionTrendPoint(String date, int moodScore, String dominantMood) {}
