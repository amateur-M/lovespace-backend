package com.meng.lovespace.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meng.lovespace.ai.rag.config.RagAiProperties;
import com.meng.lovespace.user.entity.LoveQaDocument;
import com.meng.lovespace.user.exception.LoveQaBusinessException;
import com.meng.lovespace.user.mapper.LoveQaDocumentMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 恋爱知识库文档级去重（content_hash / source_url + couple_id）。 */
@Service
@RequiredArgsConstructor
public class LoveQaDocumentDedupeService {

    private static final int CONFLICT = 40994;

    private final LoveQaDocumentMapper documentMapper;
    private final RagAiProperties ragAiProperties;

    /**
     * 查找重复文档；REJECT 策略下冲突则抛业务异常。
     *
     * @return 若 UPDATE 策略且存在重复，返回已有文档；否则 empty
     */
    public Optional<LoveQaDocument> findDuplicateOrReject(
            String contentHash, String sourceUrl, String coupleId) {
        Optional<LoveQaDocument> byUrl = findBySourceUrl(sourceUrl, coupleId);
        if (byUrl.isPresent()) {
            return handleDuplicate(byUrl.get());
        }
        Optional<LoveQaDocument> byHash = findByContentHash(contentHash, coupleId);
        if (byHash.isPresent()) {
            return handleDuplicate(byHash.get());
        }
        return Optional.empty();
    }

    private Optional<LoveQaDocument> handleDuplicate(LoveQaDocument existing) {
        if (isUpdateStrategy()) {
            return Optional.of(existing);
        }
        throw new LoveQaBusinessException(
                CONFLICT, "相同来源或内容的文档已存在（documentId=" + existing.getDocumentId() + "）");
    }

    /**
     * 获取来源 URL 的文档
     */
    private Optional<LoveQaDocument> findBySourceUrl(String sourceUrl, String coupleId) {
        if (!StringUtils.hasText(sourceUrl)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<LoveQaDocument> qw = baseCoupleQuery(coupleId);
        qw.eq(LoveQaDocument::getSourceUrl, sourceUrl.trim());
        qw.ne(LoveQaDocument::getStatus, "FAILED");
        qw.orderByDesc(LoveQaDocument::getUpdatedAt);
        qw.last("LIMIT 1");
        return Optional.ofNullable(documentMapper.selectOne(qw));
    }

    /**
     * 获取内容 SHA-256 的文档
     */
    private Optional<LoveQaDocument> findByContentHash(String contentHash, String coupleId) {
        if (!StringUtils.hasText(contentHash)) {
            return Optional.empty();
        }
        LambdaQueryWrapper<LoveQaDocument> qw = baseCoupleQuery(coupleId);
        qw.eq(LoveQaDocument::getContentHash, contentHash);
        qw.ne(LoveQaDocument::getStatus, "FAILED");
        qw.orderByDesc(LoveQaDocument::getUpdatedAt);
        qw.last("LIMIT 1");
        return Optional.ofNullable(documentMapper.selectOne(qw));
    }

    /**
     * 创建查询条件；couple_id 为空时查询为 null 的文档。
     */
    private static LambdaQueryWrapper<LoveQaDocument> baseCoupleQuery(String coupleId) {
        LambdaQueryWrapper<LoveQaDocument> qw = new LambdaQueryWrapper<>();
        // 当 coupleId 有文本内容时，拼接 eq 条件 当 coupleId 没有文本内容时，拼接 isNull 条件
        return qw.eq(StringUtils.hasText(coupleId), LoveQaDocument::getCoupleId, coupleId)
                .isNull(!StringUtils.hasText(coupleId), LoveQaDocument::getCoupleId);
    }

    private boolean isUpdateStrategy() {
        return "UPDATE".equalsIgnoreCase(ragAiProperties.getDocumentDedupeStrategy());
    }
}
