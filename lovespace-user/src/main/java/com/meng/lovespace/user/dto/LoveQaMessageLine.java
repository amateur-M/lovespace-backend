package com.meng.lovespace.user.dto;

import com.meng.lovespace.ai.dto.RetrievedChunk;
import java.time.LocalDateTime;
import java.util.List;

/** 恋爱问答单条历史消息。 */
public record LoveQaMessageLine(
        long id,
        String role,
        String content,
        LocalDateTime createdAt,
        List<RetrievedChunk> retrievedChunks) {}
