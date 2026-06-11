package com.meng.lovespace.ai.api;

import com.meng.lovespace.ai.dto.LoveQaChatParams;
import com.meng.lovespace.ai.dto.LoveQaChatResult;
import java.util.Map;

/**
 * 恋爱知识库 RAG：文档入库与多轮问答（由 {@link com.meng.lovespace.ai.rag.LoveQAService} 实现）。
 */
public interface LoveQaChatFacade {

    void ingestDocument(String text, Map<String, Object> metadata);

    /**
     * 带 documentId 台账追踪的入库：为每个 chunk 写入 documentId/chunkIndex/totalChunks metadata，失败时补偿删除已写入向量。
     *
     * @return 成功写入 Milvus 的 chunk 数
     */
    int ingestDocumentWithTracking(String documentId, String text, Map<String, Object> metadata);

    /** 按 metadata.documentId 删除 Milvus 中该文档的全部向量。 */
    void deleteVectorsByDocumentId(String documentId);

    LoveQaChatResult chat(LoveQaChatParams params);

    /** 流式多轮问答：先回调 {@link LoveQaStreamCallback#onMeta}，再多次 {@link LoveQaStreamCallback#onDelta}，最后 {@link LoveQaStreamCallback#onCompleted}。 */
    void chatStream(LoveQaChatParams params, LoveQaStreamCallback callback);
}
