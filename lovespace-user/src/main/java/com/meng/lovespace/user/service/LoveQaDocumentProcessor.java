package com.meng.lovespace.user.service;

import com.meng.lovespace.ai.api.LoveQaChatFacade;
import com.meng.lovespace.user.entity.LoveQaDocument;
import com.meng.lovespace.user.mapper.LoveQaDocumentMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 恋爱知识库文档向量入库流水线（分片 → Embedding → Milvus）。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoveQaDocumentProcessor {

    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final LoveQaDocumentMapper documentMapper;
    private final LoveQaChatFacade loveQaChatFacade;

    /**
     * 执行单条文档的向量入库；调用方须已写入台账且状态为 PENDING 或 PROCESSING。
     */
    public void processDocument(String documentId) {
        LoveQaDocument row = documentMapper.selectById(documentId);
        if (row == null) {
            log.warn("love-qa processDocument: document not found documentId={}", documentId);
            return;
        }
        if (!StringUtils.hasText(row.getContent())) {
            markFailed(row, "文档内容为空");
            return;
        }

        row.setStatus(STATUS_PROCESSING);
        row.setErrorMessage(null);
        documentMapper.updateById(row);

        Map<String, Object> meta =
                buildIngestMetadata(
                        row.getOwnerUserId(),
                        row.getTitle(),
                        row.getSourceUrl(),
                        row.getCategory(),
                        row.getCoupleId(),
                        row.getScope(),
                        null);

        long startMs = System.currentTimeMillis();
        try {
            // 文档切片
            int chunkCount =
                    loveQaChatFacade.ingestDocumentWithTracking(
                            documentId, row.getContent(), meta);
            row.setStatus(STATUS_SUCCESS);
            row.setChunkCount(chunkCount);
            row.setErrorMessage(null);
            documentMapper.updateById(row);
            log.info(
                    "love-qa document ingest success documentId={} chunkCount={} elapsedMs={}",
                    documentId,
                    chunkCount,
                    System.currentTimeMillis() - startMs);
        } catch (Exception e) {
            String err = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            markFailed(row, truncateError(err));
            log.error(
                    "love-qa document ingest failed documentId={} elapsedMs={}",
                    documentId,
                    System.currentTimeMillis() - startMs,
                    e);
        }
    }

    private void markFailed(LoveQaDocument row, String err) {
        row.setStatus(STATUS_FAILED);
        row.setErrorMessage(truncateError(err));
        documentMapper.updateById(row);
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
        if (LoveQaIngestValidator.SCOPE_COUPLE.equals(scope) && StringUtils.hasText(coupleId)) {
            meta.put("coupleId", coupleId.trim());
        }
        if (StringUtils.hasText(title)) {
            meta.put("title", title.trim());
        }
        if (StringUtils.hasText(sourceUrl)) {
            meta.put("sourceUrl", sourceUrl.trim());
        }
        if (StringUtils.hasText(category)) {
            meta.put("category", category.trim());
        }
        if (extra != null) {
            extra.forEach((k, v) -> {
                if (k != null && v != null) {
                    meta.put(k, v);
                }
            });
        }
        return meta;
    }

    private static String truncateError(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
