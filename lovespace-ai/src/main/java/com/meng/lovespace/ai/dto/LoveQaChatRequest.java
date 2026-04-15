package com.meng.lovespace.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 恋爱知识库问答请求。
 *
 * @param message 本轮输入
 * @param conversationId 可选；首轮不传则由服务端生成并在响应中返回，后续轮次原样传回以实现多轮记忆
 * @param coupleId 可选情侣绑定 ID（与 GET /couple/info 的 bindingId 一致）；用于会话标注与校验
 */
public record LoveQaChatRequest(
        @NotBlank(message = "message 不能为空") String message,
        String conversationId,
        String coupleId) {}
