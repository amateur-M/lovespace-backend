package com.meng.lovespace.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.ai.api.LoveQaChatFacade;
import com.meng.lovespace.user.dto.LoveQaDocumentDetail;
import com.meng.lovespace.user.dto.LoveQaDocumentPageResponse;
import com.meng.lovespace.user.dto.LoveQaDocumentSummary;
import com.meng.lovespace.user.dto.LoveQaIngestResponseData;
import com.meng.lovespace.user.entity.LoveQaDocument;
import com.meng.lovespace.user.exception.LoveQaBusinessException;
import com.meng.lovespace.user.mapper.LoveQaDocumentMapper;
import com.meng.lovespace.user.service.CoupleBindingService;
import com.meng.lovespace.user.service.LoveQaDocumentService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String SCOPE_COUPLE = "COUPLE";
    private static final String SCOPE_GLOBAL = "GLOBAL";

    private final LoveQaDocumentMapper documentMapper;
    private final LoveQaChatFacade loveQaChatFacade;
    private final CoupleBindingService coupleBindingService;

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

        String scope;
        String resolvedCoupleId = trimToNull(coupleId);
        if (resolvedCoupleId != null) {
            requireCoupleMembership(userId, resolvedCoupleId);
            scope = SCOPE_COUPLE;
        } else {
            scope = SCOPE_GLOBAL;
        }

        String documentId = UUID.randomUUID().toString();
        String contentHash = sha256Hex(text);

        LoveQaDocument row = new LoveQaDocument();
        row.setDocumentId(documentId);
        row.setCoupleId(resolvedCoupleId);
        row.setOwnerUserId(userId);
        row.setTitle(trimToNull(title));
        row.setSourceUrl(trimToNull(sourceUrl));
        row.setCategory(trimToNull(category));
        row.setScope(scope);
        row.setContent(text);
        row.setContentHash(contentHash);
        row.setStatus(STATUS_PENDING);
        row.setChunkCount(0);
        documentMapper.insert(row);

        row.setStatus(STATUS_PROCESSING);
        documentMapper.updateById(row);

        Map<String, Object> meta = buildIngestMetadata(
                userId, title, sourceUrl, category, resolvedCoupleId, scope, extraMetadata);

        try {
            int chunkCount = loveQaChatFacade.ingestDocumentWithTracking(documentId, text, meta);
            row.setStatus(STATUS_SUCCESS);
            row.setChunkCount(chunkCount);
            row.setErrorMessage(null);
            documentMapper.updateById(row);
            log.info(
                    "love-qa document ingest success documentId={} chunkCount={} coupleId={}",
                    documentId,
                    chunkCount,
                    resolvedCoupleId);
            return new LoveQaIngestResponseData(documentId, STATUS_SUCCESS, chunkCount);
        } catch (Exception e) {
            String err = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            row.setStatus(STATUS_FAILED);
            row.setErrorMessage(truncateError(err));
            documentMapper.updateById(row);
            log.error("love-qa document ingest failed documentId={}", documentId, e);
            throw new LoveQaBusinessException(50093, "文档入库失败：" + truncateError(err));
        }
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
        doc.setStatus(STATUS_PROCESSING);
        doc.setErrorMessage(null);
        doc.setChunkCount(0);
        documentMapper.updateById(doc);

        Map<String, Object> meta =
                buildIngestMetadata(
                        doc.getOwnerUserId(),
                        doc.getTitle(),
                        doc.getSourceUrl(),
                        doc.getCategory(),
                        doc.getCoupleId(),
                        doc.getScope(),
                        null);

        try {
            int chunkCount =
                    loveQaChatFacade.ingestDocumentWithTracking(
                            doc.getDocumentId(), doc.getContent(), meta);
            doc.setStatus(STATUS_SUCCESS);
            doc.setChunkCount(chunkCount);
            documentMapper.updateById(doc);
            log.info("love-qa document reingest success documentId={} chunkCount={}", documentId, chunkCount);
            return new LoveQaIngestResponseData(doc.getDocumentId(), STATUS_SUCCESS, chunkCount);
        } catch (Exception e) {
            String err = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            doc.setStatus(STATUS_FAILED);
            doc.setErrorMessage(truncateError(err));
            documentMapper.updateById(doc);
            log.error("love-qa document reingest failed documentId={}", documentId, e);
            throw new LoveQaBusinessException(50093, "文档重入库失败：" + truncateError(err));
        }
    }

    private LoveQaDocument requireReadableDocument(String userId, String documentId) {
        LoveQaDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new LoveQaBusinessException(NOT_FOUND, "文档不存在");
        }
        verifyReadAccess(userId, doc);
        return doc;
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

    private void requireCoupleMembership(String userId, String coupleId) {
        coupleBindingService
                .findActiveOrFrozenMembership(userId, coupleId)
                .orElseThrow(() -> new LoveQaBusinessException(FORBIDDEN, "无权为该情侣入库或查看文档"));
    }

    private static Map<String, Object> buildIngestMetadata(
            String ownerUserId,
            String title,
            String sourceUrl,
            String category,
            String coupleId,
            String scope,
            Map<String, Object> extra) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("ownerUserId", ownerUserId);
        meta.put("scope", scope);
        if (StringUtils.hasText(title)) {
            meta.put("title", title.trim());
        }
        if (StringUtils.hasText(sourceUrl)) {
            meta.put("sourceUrl", sourceUrl.trim());
        }
        if (StringUtils.hasText(category)) {
            meta.put("category", category.trim());
        }
        if (StringUtils.hasText(coupleId)) {
            meta.put("coupleId", coupleId.trim());
        }
        if (extra != null) {
            meta.putAll(extra);
        }
        return meta;
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

    private static String truncateError(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }

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
