package com.meng.lovespace.user.service;

import com.meng.lovespace.user.dto.LoveQaConversationPageResponse;
import com.meng.lovespace.user.dto.LoveQaMessagesResponse;

/** 恋爱问答会话与消息的 MySQL 持久化及查询。 */
public interface LoveQaConversationService {

    /**
     * 每轮对话成功后写入：会话元数据（首条建会话）与 user/assistant 两条消息。
     *
     * @param conversationId 与 Redis / API 一致
     * @param userId 当前用户
     * @param coupleId 可选情侣 ID
     * @param userMessage 用户本轮输入
     * @param assistantReply 模型回复
     */
    void appendChatRound(
            String conversationId,
            String userId,
            String coupleId,
            String userMessage,
            String assistantReply);

    /** 当前用户的会话列表（按更新时间倒序）。 */
    LoveQaConversationPageResponse pageConversations(String userId, long page, long pageSize);

    /** 会话下全部消息；会话须属于当前用户。 */
    LoveQaMessagesResponse listMessages(String userId, String conversationId);
}
