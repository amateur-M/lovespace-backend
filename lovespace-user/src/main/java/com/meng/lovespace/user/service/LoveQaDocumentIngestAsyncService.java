package com.meng.lovespace.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** 恋爱知识库异步入库调度。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoveQaDocumentIngestAsyncService {

    private final LoveQaDocumentProcessor documentProcessor;

    @Async
    public void processDocumentAsync(String documentId) {
        log.debug("love-qa async ingest start documentId={}", documentId);
        documentProcessor.processDocument(documentId);
    }
}
