package com.meng.lovespace.ai.dto;

import java.util.List;

/**
 * 恋爱问答响应：模型回复、会话 ID 与本轮检索引用快照（供 MySQL 持久化）。
 */
public record LoveQaChatResult(String reply, String conversationId, List<RetrievedChunk> retrievedChunks) {}
