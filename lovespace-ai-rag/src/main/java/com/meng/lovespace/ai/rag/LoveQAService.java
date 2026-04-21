package com.meng.lovespace.ai.rag;

import com.meng.lovespace.ai.api.LoveQaChatFacade;
import com.meng.lovespace.ai.dto.LoveQaChatParams;
import com.meng.lovespace.ai.dto.LoveQaChatResult;
import com.meng.lovespace.ai.exception.LoveQaConversationAccessException;
import com.meng.lovespace.ai.exception.LoveQaConversationNotFoundException;
import com.meng.lovespace.ai.provider.LLMProvider;
import com.meng.lovespace.ai.rag.config.RagAiProperties;
import com.meng.lovespace.ai.service.LlmRouter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * 恋爱知识库 RAG：文档入库与基于检索上下文的问答；支持 Redis 多轮会话记忆。
 *
 * <p>由 {@link com.meng.lovespace.ai.rag.config.LoveQaRagBeansConfiguration} 在存在 {@link VectorStore} 时注册。
 */
@Slf4j
@RequiredArgsConstructor
public class LoveQAService implements LoveQaChatFacade {

    private static final String RAG_SYSTEM_PREFIX =
            "你是一位恋爱与情感领域的助手。请仅根据下面「检索到的上下文」与「本轮之前的对话」回答用户问题；若上下文不足以回答，请明确说明，不要编造事实。\n\n";

    private final VectorStore vectorStore;
    private final DocumentIngestPipeline documentIngestPipeline;
    private final LlmRouter llmRouter;
    private final RagAiProperties ragAiProperties;
    private final LoveQAConversationStore conversationStore;

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
    public LoveQaChatResult chat(LoveQaChatParams params) {
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

        List<LoveQAConversationTurn> priorTurns = new ArrayList<>(state.getTurns());
        String historyBlock = formatHistory(priorTurns);

        int topK = Math.max(1, ragAiProperties.getRetrieveTopK());
        SearchRequest request = SearchRequest.builder().query(message).topK(topK).build();
        List<Document> hits = vectorStore.similaritySearch(request);
        String context =
                hits.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n---\n"));

        LLMProvider llm = llmRouter.resolve();
        StringBuilder system = new StringBuilder(RAG_SYSTEM_PREFIX);
        if (!historyBlock.isEmpty()) {
            system.append("【本轮之前的对话】\n").append(historyBlock).append("\n\n");
        }
        system.append("【检索到的上下文】\n").append(context);
        String reply = llm.chatWithSystem(system.toString(), message);

        state.getTurns().add(new LoveQAConversationTurn("user", message));
        state.getTurns().add(new LoveQAConversationTurn("assistant", reply));
        trimHistory(state);
        conversationStore.save(conversationId, state);

        return new LoveQaChatResult(reply, conversationId);
    }

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

    private static String formatHistory(List<LoveQAConversationTurn> turns) {
        if (turns == null || turns.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (LoveQAConversationTurn t : turns) {
            String label = "user".equalsIgnoreCase(t.role()) ? "用户" : "助手";
            sb.append(label).append(": ").append(t.content()).append("\n");
        }
        return sb.toString().trim();
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
