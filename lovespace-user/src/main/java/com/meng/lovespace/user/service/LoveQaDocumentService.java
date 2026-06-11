package com.meng.lovespace.user.service;

import com.meng.lovespace.user.dto.LoveQaDocumentDetail;
import com.meng.lovespace.user.dto.LoveQaDocumentPageResponse;
import com.meng.lovespace.user.dto.LoveQaIngestResponseData;
import java.util.Map;

/** 恋爱知识库文档台账：入库双写、列表、删除与重入库。 */
public interface LoveQaDocumentService {

    LoveQaIngestResponseData ingest(
            String userId,
            String text,
            String title,
            String sourceUrl,
            String category,
            String coupleId,
            Map<String, Object> extraMetadata);

    LoveQaDocumentPageResponse pageDocuments(String userId, String coupleId, long page, long pageSize);

    LoveQaDocumentDetail getDocument(String userId, String documentId);

    void deleteDocument(String userId, String documentId);

    LoveQaIngestResponseData reingest(String userId, String documentId);
}
