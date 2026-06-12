package com.meng.lovespace.user.service;

import com.meng.lovespace.ai.rag.config.RagAiProperties;
import com.meng.lovespace.ai.rag.retrieve.RagRetrievalFilterBuilder;
import com.meng.lovespace.user.dto.CoupleInfoResponse;
import com.meng.lovespace.user.exception.LoveQaBusinessException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 恋爱问答 chat 检索 scope / coupleId 校验与 Milvus filter 解析（阶段 2 P2-A）。
 *
 * <p>默认与入库一致：须绑定情侣；filter 为 {@code coupleId == binding || scope == GLOBAL}（可配置关闭 GLOBAL 回退）。
 */
@Component
@RequiredArgsConstructor
public class LoveQaChatRetrievalValidator {

    private static final int FORBIDDEN = 40393;
    private static final int BAD_REQUEST = 40093;

    private final CoupleBindingService coupleBindingService;
    private final RagAiProperties ragAiProperties;

    /**
     * 解析有效 coupleId 与 Milvus filter 表达式。
     *
     * @param userId 当前用户
     * @param requestedCoupleId 请求体 coupleId，可空（空时尝试用当前 bindingId）
     */
    public LoveQaChatRetrievalContext resolve(String userId, String requestedCoupleId) {
        String requested = trimToNull(requestedCoupleId);
        Optional<CoupleInfoResponse> coupleInfo = coupleBindingService.getCoupleInfo(userId);
        String bindingId = coupleInfo.map(CoupleInfoResponse::bindingId).orElse(null);

        if (!ragAiProperties.isAllowGlobalIngest()) {
            if (!StringUtils.hasText(bindingId)) {
                throw new LoveQaBusinessException(BAD_REQUEST, "请先绑定情侣后再使用恋爱问答");
            }
            String effectiveCoupleId = requested != null ? requested : bindingId;
            if (!effectiveCoupleId.equals(bindingId)) {
                throw new LoveQaBusinessException(FORBIDDEN, "coupleId 与当前情侣绑定不一致");
            }
            requireCoupleMembership(userId, effectiveCoupleId);
            return new LoveQaChatRetrievalContext(
                    effectiveCoupleId, buildCoupleFilter(effectiveCoupleId));
        }

        if (StringUtils.hasText(bindingId)) {
            String effectiveCoupleId = requested != null ? requested : bindingId;
            if (requested != null && !effectiveCoupleId.equals(bindingId)) {
                throw new LoveQaBusinessException(FORBIDDEN, "coupleId 与当前情侣绑定不一致");
            }
            requireCoupleMembership(userId, effectiveCoupleId);
            return new LoveQaChatRetrievalContext(
                    effectiveCoupleId, buildCoupleFilter(effectiveCoupleId));
        }

        if (requested != null) {
            throw new LoveQaBusinessException(BAD_REQUEST, "未绑定情侣时无法检索情侣私有知识");
        }
        return new LoveQaChatRetrievalContext(null, RagRetrievalFilterBuilder.globalOnly());
    }

    private String buildCoupleFilter(String coupleId) {
        if (ragAiProperties.isAllowGlobalFallback()) {
            return RagRetrievalFilterBuilder.coupleWithGlobalFallback(coupleId);
        }
        return RagRetrievalFilterBuilder.coupleOnly(coupleId);
    }

    private void requireCoupleMembership(String userId, String coupleId) {
        coupleBindingService
                .findActiveOrFrozenMembership(userId, coupleId)
                .orElseThrow(() -> new LoveQaBusinessException(FORBIDDEN, "无权访问该情侣的知识库"));
    }

    private static String trimToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** chat 检索上下文：有效 coupleId（GLOBAL 模式为 null）与 Milvus filter。 */
    public record LoveQaChatRetrievalContext(String effectiveCoupleId, String filterExpression) {}
}
