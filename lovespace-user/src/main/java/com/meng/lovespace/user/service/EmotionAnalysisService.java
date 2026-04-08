package com.meng.lovespace.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meng.lovespace.ai.provider.QwenProvider;
import com.meng.lovespace.user.dto.EmotionAnalysisReport;
import com.meng.lovespace.user.dto.EmotionTrendPoint;
import com.meng.lovespace.user.entity.LoveRecord;
import com.meng.lovespace.user.exception.TimelineBusinessException;
import com.meng.lovespace.user.timeline.LoveMood;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 基于恋爱时间轴记录的情感分析：统计分布与趋势，并调用通义千问生成综合结论与建议。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmotionAnalysisService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    /** 单次分析允许的最大区间天数（含首尾） */
    private static final int MAX_RANGE_DAYS = 366;
    /** 未指定起止日期时，默认回溯天数 */
    private static final int DEFAULT_LOOKBACK_DAYS = 30;
    /** 供给大模型的正文摘要条数上限 */
    private static final int LLM_CONTENT_SAMPLE_MAX = 20;
    /** 单条正文摘要最大字符数 */
    private static final int LLM_CONTENT_MAX_LEN = 120;

    private static final Map<String, Integer> MOOD_TO_SCORE =
            Map.of(
                    "happy", 82,
                    "excited", 88,
                    "loved", 85,
                    "calm", 58,
                    "sad", 32,
                    "missed", 38);

    /** 与 {@link com.meng.lovespace.user.timeline.LoveMood#ALLOWED} 一致，固定顺序便于前端展示 */
    private static final List<String> MOOD_KEYS = List.of("happy", "sad", "excited", "calm", "loved", "missed");

    private final LoveRecordService loveRecordService;
    private final ObjectProvider<QwenProvider> qwenProvider;
    private final ObjectMapper objectMapper;

    /**
     * 分析指定情侣在时间区间内的恋爱记录情感倾向。
     *
     * @param userId 当前用户（须为情侣成员）
     * @param coupleId 情侣绑定 ID
     * @param startDate 可选；与 endDate 均为空时默认近 {@value #DEFAULT_LOOKBACK_DAYS} 天
     * @param endDate 可选；默认为今天（业务时区）
     * @return 分析报告
     */
    public EmotionAnalysisReport analyze(String userId, String coupleId, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate end = endDate != null ? endDate : today;
        LocalDate start = startDate != null ? startDate : end.minusDays(DEFAULT_LOOKBACK_DAYS);

        if (start.isAfter(end)) {
            throw new TimelineBusinessException(40054, "startDate must not be after endDate");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new TimelineBusinessException(40055, "date range too large, max " + MAX_RANGE_DAYS + " days");
        }

        List<LoveRecord> records = loveRecordService.listVisibleRecordsInRange(userId, coupleId, start, end);
        Map<String, Double> distribution = computeEmotionDistribution(records);
        List<EmotionTrendPoint> trend = computeTrend(records);

        if (records.isEmpty()) {
            return new EmotionAnalysisReport(
                    "neutral",
                    50,
                    emptyDistribution(),
                    List.of(),
                    "所选时间范围内暂无恋爱记录，可先一起记录心情与日常，再查看情感趋势。");
        }

        int ruleScore = averageMoodScore(records);
        String ruleOverall = overallMoodFromScore(ruleScore);

        QwenProvider qwen = qwenProvider.getIfAvailable();
        if (qwen == null) {
            return new EmotionAnalysisReport(
                    ruleOverall,
                    ruleScore,
                    distribution,
                    trend,
                    "未配置通义千问（spring.ai.dashscope.api-key），以下为基于心情标签的统计结果，无 AI 解读。");
        }

        try {
            String system =
                    "你是亲密关系与情绪分析助手，只输出一个 JSON 对象，不要 Markdown，不要代码块，不要任何其它文字。"
                            + "字段：overallMood（字符串，仅允许 positive、neutral、negative 之一）、"
                            + "moodScore（0 到 100 的数）、"
                            + "insights（字符串，2～5 句简体中文，共情、积极、可执行的小建议，不要编造不存在的记录事实）。"
                            + "请严格依据用户提供的统计摘要与记录摘录进行分析。";
            String userPayload = buildLlmUserPayload(start, end, records, distribution);
            String raw = qwen.chatWithSystem(system, userPayload);
            ParsedLlm parsed = parseLlmJson(raw, ruleScore);
            String overall = normalizeOverallMood(parsed.overallMood(), ruleOverall);
            int score = parsed.moodScore() != null ? clampScore(parsed.moodScore()) : ruleScore;
            String insights =
                    StringUtils.hasText(parsed.insights())
                            ? truncate(parsed.insights().trim(), 3000)
                            : "保持沟通与记录习惯，关注彼此情绪变化。";
            return new EmotionAnalysisReport(overall, score, distribution, trend, insights);
        } catch (Exception e) {
            log.warn("emotion analysis LLM failed, fallback to statistics: {}", e.toString());
            return new EmotionAnalysisReport(
                    ruleOverall,
                    ruleScore,
                    distribution,
                    trend,
                    "AI 分析暂时不可用，以下为基于心情标签的统计结果。请稍后重试以获取个性化建议。");
        }
    }

    private static Map<String, Double> emptyDistribution() {
        Map<String, Double> m = new LinkedHashMap<>();
        for (String mood : MOOD_KEYS) {
            m.put(mood, 0.0);
        }
        return m;
    }

    private static Map<String, Double> computeEmotionDistribution(List<LoveRecord> records) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String mood : MOOD_KEYS) {
            counts.put(mood, 0L);
        }
        int total = 0;
        for (LoveRecord r : records) {
            String m = normalizeMood(r.getMood());
            counts.merge(m, 1L, Long::sum);
            total++;
        }
        Map<String, Double> pct = new LinkedHashMap<>();
        if (total == 0) {
            return emptyDistribution();
        }
        for (String mood : MOOD_KEYS) {
            long c = counts.getOrDefault(mood, 0L);
            pct.put(mood, Math.round(c * 1000.0 / total) / 10.0);
        }
        return pct;
    }

    private static List<EmotionTrendPoint> computeTrend(List<LoveRecord> records) {
        Map<LocalDate, List<LoveRecord>> byDay =
                records.stream().collect(Collectors.groupingBy(LoveRecord::getRecordDate));
        List<LocalDate> days = new ArrayList<>(byDay.keySet());
        days.sort(Comparator.naturalOrder());
        List<EmotionTrendPoint> out = new ArrayList<>();
        for (LocalDate d : days) {
            List<LoveRecord> dayRecords = byDay.get(d);
            int dayScore = averageMoodScore(dayRecords);
            String dominant = dominantMood(dayRecords);
            out.add(new EmotionTrendPoint(d.toString(), dayScore, dominant));
        }
        return out;
    }

    private static String dominantMood(List<LoveRecord> dayRecords) {
        Map<String, Long> cnt =
                dayRecords.stream()
                        .collect(Collectors.groupingBy(r -> normalizeMood(r.getMood()), Collectors.counting()));
        return cnt.entrySet().stream()
                .max(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .orElse("calm");
    }

    private static int averageMoodScore(List<LoveRecord> records) {
        if (records.isEmpty()) {
            return 50;
        }
        int sum = 0;
        for (LoveRecord r : records) {
            sum += MOOD_TO_SCORE.getOrDefault(normalizeMood(r.getMood()), 58);
        }
        return Math.min(100, Math.max(0, (int) Math.round((double) sum / records.size())));
    }

    private static String normalizeMood(String mood) {
        if (LoveMood.isAllowed(mood)) {
            return mood;
        }
        return "calm";
    }

    private static String overallMoodFromScore(int score) {
        if (score >= 70) {
            return "positive";
        }
        if (score >= 40) {
            return "neutral";
        }
        return "negative";
    }

    private static String normalizeOverallMood(String fromLlm, String fallback) {
        if (!StringUtils.hasText(fromLlm)) {
            return fallback;
        }
        String v = fromLlm.trim().toLowerCase(Locale.ROOT);
        if ("positive".equals(v) || "neutral".equals(v) || "negative".equals(v)) {
            return v;
        }
        return fallback;
    }

    private static int clampScore(int s) {
        return Math.min(100, Math.max(0, s));
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }

    private static String buildLlmUserPayload(
            LocalDate start,
            LocalDate end,
            List<LoveRecord> records,
            Map<String, Double> distribution) {
        StringBuilder sb = new StringBuilder();
        sb.append("【统计区间】").append(start).append(" 至 ").append(end).append('\n');
        sb.append("【记录条数】").append(records.size()).append('\n');
        sb.append("【心情标签占比（百分比）】");
        distribution.forEach((k, v) -> sb.append(k).append(':').append(v).append("%; "));
        sb.append('\n');
        sb.append("【记录正文摘录】（用于理解语境，请勿编造未出现的日期或事件）\n");
        int n = 0;
        for (LoveRecord r : records) {
            if (n >= LLM_CONTENT_SAMPLE_MAX) {
                break;
            }
            String excerpt = r.getContent() == null ? "" : r.getContent().replaceAll("\\s+", " ").trim();
            if (excerpt.length() > LLM_CONTENT_MAX_LEN) {
                excerpt = excerpt.substring(0, LLM_CONTENT_MAX_LEN) + "…";
            }
            sb.append(n + 1)
                    .append(". ")
                    .append(r.getRecordDate())
                    .append(" | ")
                    .append(normalizeMood(r.getMood()))
                    .append(" | ")
                    .append(excerpt)
                    .append('\n');
            n++;
        }
        return sb.toString();
    }

    private ParsedLlm parseLlmJson(String raw, int fallbackScore) throws Exception {
        String json = stripMarkdownFence(raw == null ? "" : raw.trim());
        JsonNode root = objectMapper.readTree(json);
        String overallMood = root.path("overallMood").asText(null);
        Integer moodScore = null;
        if (root.hasNonNull("moodScore") && root.get("moodScore").isNumber()) {
            moodScore = (int) Math.round(root.get("moodScore").asDouble());
        } else if (root.hasNonNull("moodScore")) {
            try {
                moodScore = Integer.parseInt(root.get("moodScore").asText().trim());
            } catch (NumberFormatException ignored) {
                moodScore = fallbackScore;
            }
        }
        String insights = root.path("insights").asText(null);
        return new ParsedLlm(overallMood, moodScore, insights);
    }

    private static String stripMarkdownFence(String raw) {
        String t = raw.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            int lastFence = t.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                t = t.substring(firstNl + 1, lastFence).trim();
            }
        }
        return t;
    }

    private record ParsedLlm(String overallMood, Integer moodScore, String insights) {}
}
