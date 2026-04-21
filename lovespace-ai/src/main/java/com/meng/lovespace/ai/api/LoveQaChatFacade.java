package com.meng.lovespace.ai.api;

import com.meng.lovespace.ai.dto.LoveQaChatParams;
import com.meng.lovespace.ai.dto.LoveQaChatResult;
import java.util.Map;

/**
 * 恋爱知识库 RAG：文档入库与多轮问答（由 {@code lovespace-ai-rag} 模块的 {@code LoveQAService} 实现）。
 */
public interface LoveQaChatFacade {

    void ingestDocument(String text, Map<String, Object> metadata);

    LoveQaChatResult chat(LoveQaChatParams params);

    /** 流式多轮问答：先回调 {@link LoveQaStreamCallback#onMeta}，再多次 {@link LoveQaStreamCallback#onDelta}，最后 {@link LoveQaStreamCallback#onCompleted}。 */
    void chatStream(LoveQaChatParams params, LoveQaStreamCallback callback);
}
