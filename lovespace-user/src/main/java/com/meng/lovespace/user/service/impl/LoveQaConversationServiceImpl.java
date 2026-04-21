package com.meng.lovespace.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.ai.rag.LoveQAConversationState;
import com.meng.lovespace.ai.rag.LoveQAConversationStore;
import com.meng.lovespace.ai.rag.LoveQAConversationTurn;
import com.meng.lovespace.ai.rag.config.RagAiProperties;
import com.meng.lovespace.user.dto.LoveQaConversationPageResponse;
import com.meng.lovespace.user.dto.LoveQaConversationSummary;
import com.meng.lovespace.user.dto.LoveQaMessageLine;
import com.meng.lovespace.user.dto.LoveQaMessagesResponse;
import com.meng.lovespace.user.entity.LoveQaConversation;
import com.meng.lovespace.user.entity.LoveQaMessage;
import com.meng.lovespace.user.exception.LoveQaBusinessException;
import com.meng.lovespace.user.mapper.LoveQaConversationMapper;
import com.meng.lovespace.user.mapper.LoveQaMessageMapper;
import com.meng.lovespace.user.service.LoveQaConversationService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoveQaConversationServiceImpl implements LoveQaConversationService {

    private static final int TITLE_MAX = 80;

    private final LoveQaConversationMapper conversationMapper;
    private final LoveQaMessageMapper messageMapper;
    private final LoveQAConversationStore conversationStore;
    private final RagAiProperties ragAiProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appendChatRound(
            String conversationId,
            String userId,
            String coupleId,
            String userMessage,
            String assistantReply) {
        LoveQaConversation existing = conversationMapper.selectById(conversationId);
        if (existing == null) {
            LoveQaConversation row = new LoveQaConversation();
            row.setConversationId(conversationId);
            row.setUserId(userId);
            row.setCoupleId(coupleId);
            row.setTitle(truncateTitle(userMessage));
            conversationMapper.insert(row);
        } else {
            if (!userId.equals(existing.getUserId())) {
                log.warn("appendChatRound: user mismatch conversationId={}", conversationId);
                return;
            }
            if (!StringUtils.hasText(existing.getCoupleId()) && StringUtils.hasText(coupleId)) {
                existing.setCoupleId(coupleId);
            }
            conversationMapper.updateById(existing);
        }

        insertMessage(conversationId, "user", userMessage);
        insertMessage(conversationId, "assistant", assistantReply);
    }

    private void insertMessage(String conversationId, String role, String content) {
        LoveQaMessage m = new LoveQaMessage();
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content);
        messageMapper.insert(m);
    }

    private static String truncateTitle(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        String t = message.strip().replace('\n', ' ');
        if (t.length() <= TITLE_MAX) {
            return t;
        }
        return t.substring(0, TITLE_MAX - 3) + "...";
    }

    @Override
    public LoveQaConversationPageResponse pageConversations(String userId, long page, long pageSize) {
        Page<LoveQaConversation> p = Page.of(page, pageSize);
        LambdaQueryWrapper<LoveQaConversation> q = new LambdaQueryWrapper<>();
        q.eq(LoveQaConversation::getUserId, userId).orderByDesc(LoveQaConversation::getUpdatedAt);
        Page<LoveQaConversation> result = conversationMapper.selectPage(p, q);
        List<LoveQaConversationSummary> items =
                result.getRecords().stream()
                        .map(
                                c ->
                                        new LoveQaConversationSummary(
                                                c.getConversationId(), c.getTitle(), c.getUpdatedAt()))
                        .collect(Collectors.toList());
        return new LoveQaConversationPageResponse(
                result.getTotal(), result.getCurrent(), result.getSize(), items);
    }

    @Override
    public LoveQaMessagesResponse listMessages(String userId, String conversationId) {
        LoveQaConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new LoveQaBusinessException(40492, "会话不存在");
        }
        if (!userId.equals(conv.getUserId())) {
            throw new LoveQaBusinessException(40392, "无权查看该会话");
        }
        LambdaQueryWrapper<LoveQaMessage> q = new LambdaQueryWrapper<>();
        q.eq(LoveQaMessage::getConversationId, conversationId).orderByAsc(LoveQaMessage::getId);
        List<LoveQaMessage> rows = messageMapper.selectList(q);
        List<LoveQaMessageLine> lines =
                rows.stream()
                        .map(
                                m ->
                                        new LoveQaMessageLine(
                                                m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                        .collect(Collectors.toList());
        return new LoveQaMessagesResponse(conversationId, lines);
    }

    @Override
    public void restoreRedisSessionIfMissing(String userId, String coupleId, String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        String id = conversationId.trim();
        if (conversationStore.find(id).isPresent()) {
            return;
        }
        LoveQaConversation conv = conversationMapper.selectById(id);
        if (conv == null) {
            return;
        }
        if (!userId.equals(conv.getUserId())) {
            return;
        }
        LambdaQueryWrapper<LoveQaMessage> q = new LambdaQueryWrapper<>();
        q.eq(LoveQaMessage::getConversationId, id).orderByAsc(LoveQaMessage::getId);
        List<LoveQaMessage> rows = messageMapper.selectList(q);
        LoveQAConversationState state = new LoveQAConversationState();
        state.setUserId(conv.getUserId());
        state.setCoupleId(StringUtils.hasText(conv.getCoupleId()) ? conv.getCoupleId() : coupleId);
        List<LoveQAConversationTurn> turns = new ArrayList<>();
        for (LoveQaMessage m : rows) {
            if (m.getRole() == null || m.getContent() == null) {
                continue;
            }
            String r = m.getRole().trim();
            if (!"user".equalsIgnoreCase(r) && !"assistant".equalsIgnoreCase(r)) {
                continue;
            }
            String normalized = "user".equalsIgnoreCase(r) ? "user" : "assistant";
            turns.add(new LoveQAConversationTurn(normalized, m.getContent()));
        }
        int maxPairs = Math.max(1, ragAiProperties.getMaxHistoryPairs());
        int maxMessages = maxPairs * 2;
        while (turns.size() > maxMessages) {
            turns.remove(0);
        }
        state.setTurns(turns);
        conversationStore.save(id, state);
        log.debug("restoreRedisSessionIfMissing: rehydrated conversationId={} turns={}", id, turns.size());
    }
}
