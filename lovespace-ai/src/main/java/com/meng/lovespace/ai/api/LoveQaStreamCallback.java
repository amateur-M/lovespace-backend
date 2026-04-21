package com.meng.lovespace.ai.api;

/**
 * 恋爱问答 SSE 流式输出回调：先 {@link #onMeta}，再多次 {@link #onDelta}，最后 {@link #onCompleted}。
 */
public interface LoveQaStreamCallback {

    /** 首轮事件：告知会话 ID，前端需在下轮请求中原样传回。 */
    void onMeta(String conversationId);

    /** 模型增量文本（通义为 incremental 片段；回退实现可能为整段一次）。 */
    void onDelta(String text);

    /** 流结束且 Redis 已写入本轮记忆后的完整回复（与增量拼接结果一致）。 */
    void onCompleted(String fullReply);
}
