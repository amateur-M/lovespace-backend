package com.meng.lovespace.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.ai.api.LoveQaChatFacade;
import com.meng.lovespace.ai.rag.config.RagAiProperties;
import com.meng.lovespace.user.dto.LoveQaDocumentDetail;
import com.meng.lovespace.user.dto.LoveQaDocumentPageResponse;
import com.meng.lovespace.user.dto.LoveQaDocumentSummary;
import com.meng.lovespace.user.dto.LoveQaIngestResponseData;
import com.meng.lovespace.user.entity.LoveQaDocument;
import com.meng.lovespace.user.exception.LoveQaBusinessException;
import com.meng.lovespace.user.mapper.LoveQaDocumentMapper;
import com.meng.lovespace.user.service.CoupleBindingService;
import com.meng.lovespace.user.service.LoveQaDocumentDedupeService;
import com.meng.lovespace.user.service.LoveQaDocumentIngestAsyncService;
import com.meng.lovespace.user.service.LoveQaDocumentProcessor;
import com.meng.lovespace.user.service.LoveQaDocumentService;
import com.meng.lovespace.user.service.LoveQaIngestValidator;
import com.meng.lovespace.user.service.LoveQaIngestValidator.LoveQaIngestScope;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoveQaDocumentServiceImpl implements LoveQaDocumentService {

    private static final int NOT_FOUND = 40493;
    private static final int FORBIDDEN = 40393;
    private static final int BAD_REQUEST = 40093;

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final LoveQaDocumentMapper documentMapper;
    private final LoveQaChatFacade loveQaChatFacade;
    private final CoupleBindingService coupleBindingService;
    private final LoveQaIngestValidator ingestValidator;
    private final LoveQaDocumentDedupeService dedupeService;
    private final LoveQaDocumentProcessor documentProcessor;
    private final LoveQaDocumentIngestAsyncService ingestAsyncService;
    private final RagAiProperties ragAiProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoveQaIngestResponseData ingest(
            String userId,
            String text,
            String title,
            String sourceUrl,
            String category,
            String coupleId,
            Map<String, Object> extraMetadata) {
        if (!StringUtils.hasText(text) || text.isBlank()) {
            throw new LoveQaBusinessException(BAD_REQUEST, "文档内容不能为空");
        }

        // 确定是否允许导入全局知识库
        LoveQaIngestScope ingestScope = ingestValidator.resolve(userId, coupleId);
        String scope = ingestScope.scope();
        String resolvedCoupleId = ingestScope.coupleId();
        String contentHash = sha256Hex(text);
        String trimmedSourceUrl = trimToNull(sourceUrl);

        // 查询是否存在重复的文档（根据文档级去重策略）
        Optional<LoveQaDocument> duplicate =
                dedupeService.findDuplicateOrReject(contentHash, trimmedSourceUrl, resolvedCoupleId);

        LoveQaDocument row;
        if (duplicate.isPresent()) {
            row = duplicate.get();
            // 根据 DocumentId 删除已有的向量
            loveQaChatFacade.deleteVectorsByDocumentId(row.getDocumentId());
            applyIngestFields(row, userId, text, title, trimmedSourceUrl, category, scope, resolvedCoupleId, contentHash);
            row.setStatus(STATUS_PENDING);
            row.setChunkCount(0);
            row.setErrorMessage(null);
            documentMapper.updateById(row);
            log.info("love-qa document dedupe UPDATE documentId={}", row.getDocumentId());
        } else {
            row = new LoveQaDocument();
            row.setDocumentId(UUID.randomUUID().toString());
            applyIngestFields(row, userId, text, title, trimmedSourceUrl, category, scope, resolvedCoupleId, contentHash);
            row.setStatus(STATUS_PENDING);
            row.setChunkCount(0);
            documentMapper.insert(row);
        }

        return dispatchIngest(row.getDocumentId());
    }

    @Override
    public LoveQaDocumentPageResponse pageDocuments(String userId, String coupleId, long page, long pageSize) {
        LambdaQueryWrapper<LoveQaDocument> qw = new LambdaQueryWrapper<>();
        String resolvedCoupleId = trimToNull(coupleId);
        if (resolvedCoupleId != null) {
            requireCoupleMembership(userId, resolvedCoupleId);
            qw.eq(LoveQaDocument::getCoupleId, resolvedCoupleId);
        } else {
            qw.eq(LoveQaDocument::getOwnerUserId, userId);
        }
        qw.orderByDesc(LoveQaDocument::getUpdatedAt);

        Page<LoveQaDocument> p = documentMapper.selectPage(new Page<>(page, pageSize), qw);
        List<LoveQaDocumentSummary> items =
                p.getRecords().stream().map(this::toSummary).collect(Collectors.toList());
        return new LoveQaDocumentPageResponse(p.getTotal(), p.getCurrent(), p.getSize(), items);
    }

    @Override
    public LoveQaDocumentDetail getDocument(String userId, String documentId) {
        LoveQaDocument doc = requireReadableDocument(userId, documentId);
        return toDetail(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(String userId, String documentId) {
        LoveQaDocument doc = requireReadableDocument(userId, documentId);
        loveQaChatFacade.deleteVectorsByDocumentId(doc.getDocumentId());
        documentMapper.deleteById(doc.getDocumentId());
        log.info("love-qa document deleted documentId={} userId={}", documentId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoveQaIngestResponseData reingest(String userId, String documentId) {
        LoveQaDocument doc = requireReadableDocument(userId, documentId);
        if (!StringUtils.hasText(doc.getContent())) {
            throw new LoveQaBusinessException(BAD_REQUEST, "文档无内容快照，无法重入库");
        }

        loveQaChatFacade.deleteVectorsByDocumentId(doc.getDocumentId());
        doc.setStatus(STATUS_PENDING);
        doc.setErrorMessage(null);
        doc.setChunkCount(0);
        documentMapper.updateById(doc);

        return dispatchIngest(doc.getDocumentId());
    }

    private LoveQaIngestResponseData dispatchIngest(String documentId) {
        // 异步入库
        if (ragAiProperties.isAsyncIngest()) {
            ingestAsyncService.processDocumentAsync(documentId);
            return new LoveQaIngestResponseData(documentId, STATUS_PENDING, 0);
        }
        documentProcessor.processDocument(documentId);
        LoveQaDocument done = documentMapper.selectById(documentId);
        if (done == null) {
            throw new LoveQaBusinessException(50093, "文档入库失败");
        }
        if (!STATUS_SUCCESS.equals(done.getStatus())) {
            throw new LoveQaBusinessException(
                    50093,
                    "文档入库失败：" + (done.getErrorMessage() != null ? done.getErrorMessage() : done.getStatus()));
        }
        return new LoveQaIngestResponseData(
                documentId,
                done.getStatus(),
                done.getChunkCount() != null ? done.getChunkCount() : 0);
    }

    private static void applyIngestFields(
            LoveQaDocument row,
            String userId,
            String text,
            String title,
            String sourceUrl,
            String category,
            String scope,
            String coupleId,
            String contentHash) {
        row.setOwnerUserId(userId);
        row.setTitle(trimToNull(title));
        row.setSourceUrl(sourceUrl);
        row.setCategory(trimToNull(category));
        row.setScope(scope);
        row.setCoupleId(coupleId);
        row.setContent(text);
        row.setContentHash(contentHash);
    }

    private LoveQaDocument requireReadableDocument(String userId, String documentId) {
        LoveQaDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new LoveQaBusinessException(NOT_FOUND, "文档不存在");
        }
        verifyReadAccess(userId, doc);
        return doc;
    }

    private void requireCoupleMembership(String userId, String coupleId) {
        coupleBindingService
                .findActiveOrFrozenMembership(userId, coupleId)
                .orElseThrow(() -> new LoveQaBusinessException(FORBIDDEN, "无权查看该情侣的知识库文档"));
    }

    private void verifyReadAccess(String userId, LoveQaDocument doc) {
        if (userId.equals(doc.getOwnerUserId())) {
            return;
        }
        if (StringUtils.hasText(doc.getCoupleId())) {
            if (coupleBindingService
                    .findActiveOrFrozenMembership(userId, doc.getCoupleId())
                    .isPresent()) {
                return;
            }
        }
        throw new LoveQaBusinessException(FORBIDDEN, "无权访问该文档");
    }

    private LoveQaDocumentSummary toSummary(LoveQaDocument doc) {
        return new LoveQaDocumentSummary(
                doc.getDocumentId(),
                doc.getCoupleId(),
                doc.getTitle(),
                doc.getSourceUrl(),
                doc.getCategory(),
                doc.getScope(),
                doc.getStatus(),
                doc.getChunkCount() != null ? doc.getChunkCount() : 0,
                doc.getCreatedAt(),
                doc.getUpdatedAt());
    }

    private LoveQaDocumentDetail toDetail(LoveQaDocument doc) {
        return new LoveQaDocumentDetail(
                doc.getDocumentId(),
                doc.getCoupleId(),
                doc.getOwnerUserId(),
                doc.getTitle(),
                doc.getSourceUrl(),
                doc.getCategory(),
                doc.getScope(),
                doc.getStatus(),
                doc.getChunkCount() != null ? doc.getChunkCount() : 0,
                doc.getErrorMessage(),
                doc.getCreatedAt(),
                doc.getUpdatedAt());
    }

    private static String trimToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * 计算输入文本的SHA-256哈希值并转为十六进制字符串
     */
    private static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
