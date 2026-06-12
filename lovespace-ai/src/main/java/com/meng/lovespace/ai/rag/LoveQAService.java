package com.meng.lovespace.ai.rag;

import com.meng.lovespace.ai.api.LoveQaChatFacade;
import com.meng.lovespace.ai.api.LoveQaStreamCallback;
import com.meng.lovespace.ai.dto.ChatTurn;
import com.meng.lovespace.ai.dto.LoveQaChatParams;
import com.meng.lovespace.ai.dto.LoveQaChatResult;
import com.meng.lovespace.ai.dto.RetrievedChunk;
import com.meng.lovespace.ai.exception.LoveQaConversationAccessException;
import com.meng.lovespace.ai.exception.LoveQaConversationNotFoundException;
import com.meng.lovespace.ai.provider.LLMProvider;
import com.meng.lovespace.ai.rag.config.RagAiProperties;
import com.meng.lovespace.ai.rag.compress.PromptCompressor;
import com.meng.lovespace.ai.rag.metrics.RagMetricsCollector;
import com.meng.lovespace.ai.rag.metrics.RagPhase;
import com.meng.lovespace.ai.rag.metrics.RagTimer;
import com.meng.lovespace.ai.rag.retrieve.RagRetrievalPipeline;
import com.meng.lovespace.ai.service.LlmRouter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.StringUtils;

/**
 * 恋爱知识库 RAG：文档入库与基于检索上下文的问答；支持 Redis 多轮会话记忆。
 *
 * <p>由 {@link com.meng.lovespace.ai.rag.config.LoveQaRagBeansConfiguration} 注册为 Spring Bean。
 */
@Slf4j
@RequiredArgsConstructor
public class LoveQAService implements LoveQaChatFacade {

    private static final String RAG_SYSTEM_PREFIX =
            "你是一位恋爱与情感领域的专业助手。请严格仅根据下面「检索到的上下文」与「本轮之前的对话」回答用户问题。\n\n" +
            "【回答格式要求】\n" +
            "1. 在关键论点或建议后，必须用【1】、【2】等格式引用来源编号（对应【来源列表】中的序号）。\n" +
            "2. 若检索到的上下文不足以可靠回答，请明确回复：「根据当前知识库，我无法给出可靠答案，请提供更多细节或换个问题。」，严禁编造事实或虚构内容。\n" +
            "3. 回答要结构清晰、共情且实用，先共情再给建议。\n\n";

    /** 0 命中或阈值过滤后无片段时直接返回，不调用 LLM。 */
    static final String NO_RETRIEVAL_REPLY =
            "根据当前知识库，我无法给出可靠答案，请提供更多细节或换个问题。";

    private final VectorStore vectorStore;
    private final DocumentIngestPipeline documentIngestPipeline;
    private final LlmRouter llmRouter;
    private final RagAiProperties ragAiProperties;
    private final LoveQAConversationStore conversationStore;
    private final RagMetricsCollector metricsCollector;
    private final PromptCompressor promptCompressor;
    private final RagRetrievalPipeline retrievalPipeline;

    @Override
    public void ingestDocument(String text, Map<String, Object> metadata) {
        List<Document> docs = documentIngestPipeline.splitToDocuments(text, metadata);
        if (docs.isEmpty()) {
            log.warn("ingestDocument: no chunks produced");
            return;
        }
        vectorStore.add(docs);
        log.info("ingestDocument: added {} chunks", docs.size());
    }

    @Override
    public int ingestDocumentWithTracking(String documentId, String text, Map<String, Object> metadata) {
        if (!StringUtils.hasText(documentId)) {
            throw new IllegalArgumentException("documentId 不能为空");
        }
        List<Document> docs = documentIngestPipeline.splitToDocuments(text, metadata);
        if (docs.isEmpty()) {
            log.warn("ingestDocumentWithTracking: no chunks produced documentId={}", documentId);
            return 0;
        }

        int totalChunks = docs.size();
        String ingestedAt = Instant.now().toString();
        List<Document> enriched = new ArrayList<>(totalChunks);
        for (int i = 0; i < totalChunks; i++) {
            Document doc = docs.get(i);
            Map<String, Object> meta = new HashMap<>(doc.getMetadata() != null ? doc.getMetadata() : Map.of());
            meta.put("documentId", documentId);
            meta.put("chunkIndex", i);
            meta.put("totalChunks", totalChunks);
            meta.putIfAbsent("ingestedAt", ingestedAt);
            enriched.add(new Document(doc.getId(), doc.getText(), meta));
        }

        try {
            vectorStore.add(enriched);
            log.info("ingestDocumentWithTracking: documentId={} added {} chunks", documentId, enriched.size());
            return enriched.size();
        } catch (Exception e) {
            log.error("ingestDocumentWithTracking failed documentId={}, compensating delete", documentId, e);
            try {
                deleteVectorsByDocumentId(documentId);
            } catch (Exception ex) {
                log.error("compensate delete failed documentId={}", documentId, ex);
            }
            throw e;
        }
    }

    @Override
    public void deleteVectorsByDocumentId(String documentId) {
        if (!StringUtils.hasText(documentId)) {
            return;
        }
        String safeId = documentId.trim().replace("'", "\\'");
        vectorStore.delete("documentId == '" + safeId + "'");
        log.info("deleteVectorsByDocumentId: documentId={}", documentId);
    }

    @Override
    public LoveQaChatResult chat(LoveQaChatParams params) {
        RagTimer timer = metricsCollector.startTimer();
        PreparedChat prep = prepareChat(params, timer);
        if (prep.noRetrievalHit()) {
            persistRound(prep, NO_RETRIEVAL_REPLY, timer);
            metricsCollector.record(
                    timer, prep.conversationId(), prep.userMessage().length(), 0);
            return new LoveQaChatResult(NO_RETRIEVAL_REPLY, prep.conversationId(), List.of());
        }
        LLMProvider llm = llmRouter.resolve();

        timer.phase(RagPhase.LLM_TOTAL);
        String reply =
                llm.chatWithSystemAndHistory(prep.systemPrompt(), prep.priorForLlm(), prep.userMessage());
        timer.markLlmTotalDone();

        persistRound(prep, reply, timer);
        metricsCollector.record(timer, prep.conversationId(), 
                prep.userMessage().length(), prep.retrievedChunkCount());
        return new LoveQaChatResult(reply, prep.conversationId(), prep.retrievedChunks());
    }

    @Override
    public void chatStream(LoveQaChatParams params, LoveQaStreamCallback callback) {
        RagTimer timer = metricsCollector.startTimer();
        PreparedChat prep = prepareChat(params, timer);
        callback.onMeta(prep.conversationId());

        if (prep.noRetrievalHit()) {
            persistRound(prep, NO_RETRIEVAL_REPLY, timer);
            callback.onCompleted(NO_RETRIEVAL_REPLY);
            metricsCollector.record(
                    timer, prep.conversationId(), prep.userMessage().length(), 0);
            return;
        }

        // 发送检索结果供前端可视化
        if (prep.retrievedChunks() != null && !prep.retrievedChunks().isEmpty()) {
            callback.onRetrieved(prep.retrievedChunks());
        }

        StringBuilder acc = new StringBuilder();
        LLMProvider llm = llmRouter.resolve();

        timer.phase(RagPhase.LLM_FIRST_BYTE);
        llm.chatWithSystemAndHistoryStreaming(
                prep.systemPrompt(),
                prep.priorForLlm(),
                prep.userMessage(),
                delta -> {
                    // 记录首字节时间
                    if (timer.isFirstBytePending() && delta != null && !delta.isEmpty()) {
                        timer.markLlmFirstByte();
                    }
                    if (delta != null && !delta.isEmpty()) {
                        acc.append(delta);
                        callback.onDelta(delta);
                    }
                });
        timer.markLlmTotalDone();

        String full = acc.toString();
        persistRound(prep, full, timer);
        callback.onCompleted(full);
        metricsCollector.record(timer, prep.conversationId(),
                prep.userMessage().length(), prep.retrievedChunkCount());
    }

    private PreparedChat prepareChat(LoveQaChatParams params, RagTimer timer) {
        String message = params.message();
        String userId = params.userId();
        String coupleId = params.coupleId();

        String conversationId;
        LoveQAConversationState state;

        String existingId = params.conversationId();
        if (existingId == null || existingId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
            state = new LoveQAConversationState();
            state.setUserId(userId);
            state.setCoupleId(coupleId);
            state.setTurns(new ArrayList<>());
        } else {
            conversationId = existingId.trim();
            state =
                    conversationStore
                            .find(conversationId)
                            .orElseThrow(() -> new LoveQaConversationNotFoundException(conversationId));
            verifyAccess(state, userId, coupleId);
            if (state.getCoupleId() == null && coupleId != null) {
                state.setCoupleId(coupleId);
            }
        }

        if (state.getTurns() == null) {
            state.setTurns(new ArrayList<>());
        }
        List<LoveQAConversationTurn> priorSnapshot = new ArrayList<>(state.getTurns());

        // 向量检索：candidate-k → 阈值 → documentId 去重 → L1 rerank → final topK
        timer.phase(RagPhase.RETRIEVE);
        String filterExpression = params.retrievalFilterExpression();
        if (!StringUtils.hasText(filterExpression)) {
            log.warn(
                    "RAG retrieval without filterExpression userId={} coupleId={}",
                    userId,
                    coupleId);
        } else {
            log.debug("RAG retrieval filter: {}", filterExpression);
        }

        List<Document> hits = retrievalPipeline.retrieve(message, filterExpression);
        timer.markRetrieveDone();
        boolean noRetrievalHit = hits.isEmpty();

        // Prompt 构建阶段（含压缩）
        timer.phase(RagPhase.PROMPT_BUILD);

        List<Document> compressedHits = noRetrievalHit ? List.of() : promptCompressor.compressRetrievedDocs(hits);
        String context = compressedHits.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
        
        // 2. 压缩历史对话
        List<ChatTurn> priorForLlmRaw =
                priorSnapshot.stream().map(t -> new ChatTurn(t.role(), t.content())).toList();
        List<ChatTurn> priorForLlm = promptCompressor.compressHistory(priorForLlmRaw);
        
        // 3. 构建系统 Prompt（P1-4 增强：传入 sourceDocs 以生成来源列表，强制模型引用）
        String systemPrompt = promptCompressor.buildSystemPrompt(RAG_SYSTEM_PREFIX, context, compressedHits);
        
        timer.markPromptBuildDone();

        // 构建 RetrievedChunk 列表（用于前端可视化）
        List<RetrievedChunk> retrievedChunks = compressedHits.stream()
                .map(doc -> RetrievedChunk.fromDocument(doc, 120))
                .filter(chunk -> chunk != null)
                .collect(Collectors.toList());

        return new PreparedChat(
                conversationId,
                state,
                message,
                systemPrompt,
                priorForLlm,
                compressedHits.size(),
                retrievedChunks,
                noRetrievalHit);
    }

    private void persistRound(PreparedChat prep, String reply, RagTimer timer) {
        timer.phase(RagPhase.PERSIST);
        prep.state().getTurns().add(new LoveQAConversationTurn("user", prep.userMessage()));
        prep.state().getTurns().add(new LoveQAConversationTurn("assistant", reply));
        trimHistory(prep.state());
        conversationStore.save(prep.conversationId(), prep.state());
        timer.markPersistDone();
    }

    private record PreparedChat(
            String conversationId,
            LoveQAConversationState state,
            String userMessage,
            String systemPrompt,
            List<ChatTurn> priorForLlm,
            int retrievedChunkCount,
            List<RetrievedChunk> retrievedChunks,
            boolean noRetrievalHit) {}

    private void verifyAccess(LoveQAConversationState state, String userId, String coupleId) {
        if (state.getUserId() == null || !state.getUserId().equals(userId)) {
            throw new LoveQaConversationAccessException("无权访问该会话");
        }
        if (state.getCoupleId() != null
                && coupleId != null
                && !state.getCoupleId().equals(coupleId)) {
            throw new LoveQaConversationAccessException("情侣 ID 与会话不一致");
        }
    }

    private void trimHistory(LoveQAConversationState state) {
        int maxPairs = Math.max(1, ragAiProperties.getMaxHistoryPairs());
        int maxMessages = maxPairs * 2;
        List<LoveQAConversationTurn> turns = state.getTurns();
        while (turns.size() > maxMessages) {
            turns.remove(0);
        }
    }
}
