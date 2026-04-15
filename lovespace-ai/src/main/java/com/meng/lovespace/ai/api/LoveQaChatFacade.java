package com.meng.lovespace.ai.api;

import com.meng.lovespace.ai.dto.LoveQaChatParams;
import com.meng.lovespace.ai.dto.LoveQaChatResult;
import java.util.Map;

/**
 * 恋爱知识库 RAG：文档入库与多轮问答（由 {@code lovespace-ai-rag} 在启用 Milvus 时提供实现）。
 */
public interface LoveQaChatFacade {

    void ingestDocument(String text, Map<String, Object> metadata);

    LoveQaChatResult chat(LoveQaChatParams params);
}
