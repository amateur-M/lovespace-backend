package com.meng.lovespace.ai.api;

import com.meng.lovespace.ai.dto.RetrievedChunk;
import java.util.List;

/**
 * 恋爱问答 SSE 流式输出回调：先 {@link #onMeta}，再 {@link #onRetrieved}，
 * 然后多次 {@link #onDelta}，最后 {@link #onCompleted}。
 */
public interface LoveQaStreamCallback {

    /** 首轮事件：告知会话 ID，前端需在下轮请求中原样传回。 */
    void onMeta(String conversationId);

    /**
     * 检索完成事件：告知前端参考了哪些知识片段（用于可视化引用来源）。
     *
     * @param chunks 检索到的知识片段列表
     */
    default void onRetrieved(List<RetrievedChunk> chunks) {
        // 默认空实现，向后兼容
    }

    /** 模型增量文本（通义为 incremental 片段；回退实现可能为整段一次）。 */
    void onDelta(String text);

    /** 流结束且 Redis 已写入本轮记忆后的完整回复（与增量拼接结果一致）。 */
    void onCompleted(String fullReply);
}
