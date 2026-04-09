package com.meng.lovespace.user.service.impl;

import com.meng.lovespace.ai.provider.QwenProvider;
import com.meng.lovespace.user.dto.CoupleInfoResponse;
import com.meng.lovespace.user.entity.LoveRecord;
import com.meng.lovespace.user.entity.User;
import com.meng.lovespace.user.exception.LoveLetterBusinessException;
import com.meng.lovespace.user.service.CoupleBindingService;
import com.meng.lovespace.user.service.LoveLetterService;
import com.meng.lovespace.user.service.LoveRecordService;
import com.meng.lovespace.user.service.UserService;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * 情书生成：校验情侣成员，解析称呼与天数，组装 Prompt 并调用通义千问。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoveLetterServiceImpl implements LoveLetterService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private static final String SYSTEM_PROMPT =
            "你是一位浪漫的情感作家。请只输出情书正文，不要使用 Markdown，不要写「以下是情书」等套话标题，不要编号。";

    private static final String USER_TEMPLATE =
            """
            你是一位浪漫的情感作家，请根据以下信息生成一封情书：
            - 发送方：%s
            - 接收方：%s
            - 恋爱天数：%d天
            - 风格：%s（romantic/humorous/sincere）
            - 共同回忆：%s

            要求：字数%s，情感真挚，融入共同回忆
            """;

    private static final Map<String, String> STYLE_LABEL =
            Map.of(
                    "romantic", "浪漫（romantic）",
                    "humorous", "幽默（humorous）",
                    "sincere", "真挚（sincere）");

    private static final Map<String, String> LENGTH_HINT =
            Map.of(
                    "short", "约200字以内",
                    "medium", "约400字左右",
                    "long", "约800字左右");

    private final CoupleBindingService coupleBindingService;
    private final UserService userService;
    private final LoveRecordService loveRecordService;
    private final ObjectProvider<QwenProvider> qwenProvider;

    @Override
    public String generate(String userId, String coupleId, String style, String length, String memories) {
        coupleBindingService
                .findActiveOrFrozenMembership(userId, coupleId)
                .orElseThrow(() -> new LoveLetterBusinessException(40360, "forbidden or invalid couple"));

        CoupleInfoResponse info =
                coupleBindingService
                        .getCoupleInfo(userId)
                        .filter(i -> coupleId.equals(i.bindingId()))
                        .orElseThrow(() -> new LoveLetterBusinessException(40361, "couple info not found"));

        User sender = userService.getById(userId);
        if (sender == null) {
            throw new LoveLetterBusinessException(40460, "user not found");
        }
        String senderName = sender.getUsername() != null ? sender.getUsername() : "我";
        String receiverName =
                info.partner() != null && StringUtils.hasText(info.partner().username())
                        ? info.partner().username()
                        : "你";

        int days = info.relationshipDays();
        String styleLabel = STYLE_LABEL.getOrDefault(style.toLowerCase(Locale.ROOT), style);
        String lengthHint = LENGTH_HINT.getOrDefault(length.toLowerCase(Locale.ROOT), "约400字左右");

        String memoriesText = buildMemories(userId, coupleId, memories);

        String userMessage =
                String.format(
                        Locale.CHINA,
                        USER_TEMPLATE.trim(),
                        senderName,
                        receiverName,
                        days,
                        styleLabel,
                        memoriesText,
                        lengthHint);

        QwenProvider qwen = qwenProvider.getIfAvailable();
        if (qwen == null) {
            throw new LoveLetterBusinessException(50360, "AI 服务未配置：请设置 spring.ai.dashscope.api-key");
        }
        try {
            String content = qwen.chatWithSystem(SYSTEM_PROMPT, userMessage);
            log.info("loveLetter.generated userId={} coupleId={} style={} length={}", userId, coupleId, style, length);
            return content != null ? content.trim() : "";
        } catch (Exception e) {
            log.warn("loveLetter.generate failed: {}", e.toString());
            throw new LoveLetterBusinessException(50361, "情书生成失败，请稍后重试");
        }
    }

    private String buildMemories(String userId, String coupleId, String override) {
        if (StringUtils.hasText(override)) {
            return override.trim();
        }
        LocalDate end = LocalDate.now(BUSINESS_ZONE);
        LocalDate start = end.minusDays(365);
        List<LoveRecord> rows = loveRecordService.listVisibleRecordsInRange(userId, coupleId, start, end);
        if (rows.isEmpty()) {
            return "（暂无时间轴记录摘要，可侧重日常陪伴与未来期许）";
        }
        List<LoveRecord> tail = rows.size() > 15 ? rows.subList(rows.size() - 15, rows.size()) : rows;
        return tail.stream()
                .map(
                        r -> {
                            String d = r.getRecordDate() != null ? r.getRecordDate().toString() : "";
                            String c = r.getContent() != null ? r.getContent() : "";
                            if (c.length() > 120) {
                                c = c.substring(0, 120) + "…";
                            }
                            return d + "：" + c;
                        })
                .collect(Collectors.joining("\n"));
    }
}
