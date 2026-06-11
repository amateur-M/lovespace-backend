package com.meng.lovespace.user.service;

import com.meng.lovespace.ai.rag.config.RagAiProperties;
import com.meng.lovespace.user.dto.CoupleInfoResponse;
import com.meng.lovespace.user.exception.LoveQaBusinessException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 恋爱知识库入库 scope / coupleId 校验（阶段 1 P1-B）。
 *
 * <p>默认关闭 GLOBAL 入库：须已绑定情侣，且 coupleId 与当前用户 bindingId 一致。
 */
@Component
@RequiredArgsConstructor
public class LoveQaIngestValidator {

    private static final int FORBIDDEN = 40393;
    private static final int BAD_REQUEST = 40093;

    public static final String SCOPE_COUPLE = "COUPLE";
    public static final String SCOPE_GLOBAL = "GLOBAL";

    private final CoupleBindingService coupleBindingService;
    private final RagAiProperties ragAiProperties;

    /**
     * 解析入库 scope 与有效 coupleId。
     *
     * @param userId 当前用户
     * @param requestedCoupleId 请求体或表单中的 coupleId，可空（空时尝试用当前绑定）
     * @return 解析结果
     */
    public LoveQaIngestScope resolve(String userId, String requestedCoupleId) {
        String requested = trimToNull(requestedCoupleId);
        Optional<CoupleInfoResponse> coupleInfo = coupleBindingService.getCoupleInfo(userId);
        String bindingId = coupleInfo.map(CoupleInfoResponse::bindingId).orElse(null);

        if (!ragAiProperties.isAllowGlobalIngest()) {
            if (!StringUtils.hasText(bindingId)) {
                throw new LoveQaBusinessException(BAD_REQUEST, "请先绑定情侣后再入库知识");
            }
            String effectiveCoupleId = requested != null ? requested : bindingId;
            if (!effectiveCoupleId.equals(bindingId)) {
                throw new LoveQaBusinessException(FORBIDDEN, "coupleId 与当前情侣绑定不一致");
            }
            requireCoupleMembership(userId, effectiveCoupleId);
            return new LoveQaIngestScope(SCOPE_COUPLE, effectiveCoupleId);
        }

        if (requested != null) {
            requireCoupleMembership(userId, requested);
            return new LoveQaIngestScope(SCOPE_COUPLE, requested);
        }
        return new LoveQaIngestScope(SCOPE_GLOBAL, null);
    }

    private void requireCoupleMembership(String userId, String coupleId) {
        coupleBindingService
                .findActiveOrFrozenMembership(userId, coupleId)
                .orElseThrow(() -> new LoveQaBusinessException(FORBIDDEN, "无权为该情侣入库知识"));
    }

    private static String trimToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** 入库 scope 与 coupleId 解析结果。 */
    public record LoveQaIngestScope(String scope, String coupleId) {}
}
