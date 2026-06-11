package com.meng.lovespace.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meng.lovespace.ai.rag.config.RagAiProperties;
import com.meng.lovespace.user.entity.LoveQaDocument;
import com.meng.lovespace.user.mapper.LoveQaDocumentMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 启动时恢复 PENDING/PROCESSING 文档的异步入库任务。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoveQaIngestRecoveryRunner implements ApplicationRunner {

    private final LoveQaDocumentMapper documentMapper;
    private final LoveQaDocumentIngestAsyncService ingestAsyncService;
    private final RagAiProperties ragAiProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!ragAiProperties.isAsyncIngest()) {
            return;
        }
        LambdaQueryWrapper<LoveQaDocument> qw = new LambdaQueryWrapper<>();
        qw.in(LoveQaDocument::getStatus, List.of("PENDING", "PROCESSING"));
        List<LoveQaDocument> pending = documentMapper.selectList(qw);
        if (pending.isEmpty()) {
            return;
        }
        log.info("love-qa ingest recovery: re-queue {} documents", pending.size());
        for (LoveQaDocument doc : pending) {
            ingestAsyncService.processDocumentAsync(doc.getDocumentId());
        }
    }
}
