package com.meng.lovespace.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meng.lovespace.user.dto.MessageSendRequest;
import com.meng.lovespace.user.dto.PrivateMessageResponse;
import com.meng.lovespace.user.dto.ScheduledMessageCreateRequest;
import com.meng.lovespace.user.entity.CoupleBinding;
import com.meng.lovespace.user.entity.PrivateMessage;
import com.meng.lovespace.user.exception.MessageBusinessException;
import com.meng.lovespace.user.mapper.MessageMapper;
import com.meng.lovespace.user.service.CoupleBindingService;
import com.meng.lovespace.user.service.MessageService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link MessageService} 实现：情侣成员鉴权、消息状态流转、定时消息派发。
 */
@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, PrivateMessage> implements MessageService {

    private final CoupleBindingService coupleBindingService;

    public MessageServiceImpl(CoupleBindingService coupleBindingService) {
        this.coupleBindingService = coupleBindingService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrivateMessageResponse send(String senderId, MessageSendRequest req) {
        CoupleBinding membership = assertCoupleMember(senderId, req.coupleId());
        assertReceiverIsPartner(senderId, req.receiverId(), membership);
        validateContentAndType(req.content(), req.messageType());

        PrivateMessage row = new PrivateMessage();
        row.setCoupleId(req.coupleId());
        row.setSenderId(senderId);
        row.setReceiverId(req.receiverId());
        row.setContent(req.content().trim());
        row.setMessageType(req.messageType());
        row.setIsScheduled(0);
        row.setScheduledTime(null);
        row.setIsRead(0);
        row.setReadTime(null);
        row.setIsRetracted(0);
        save(row);
        log.info("message.sent id={} coupleId={} senderId={} receiverId={}", row.getId(), row.getCoupleId(), senderId, req.receiverId());
        return toResponse(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrivateMessageResponse createScheduled(String senderId, ScheduledMessageCreateRequest req) {
        if (isBlank(req.coupleId()) || isBlank(req.receiverId()) || isBlank(req.content()) || isBlank(req.messageType())) {
            throw new MessageBusinessException(40070, "coupleId, receiverId, content, messageType are required");
        }
        CoupleBinding membership = assertCoupleMember(senderId, req.coupleId());
        assertReceiverIsPartner(senderId, req.receiverId(), membership);
        validateContentAndType(req.content(), req.messageType());
        if (req.scheduledTime() == null || !req.scheduledTime().isAfter(LocalDateTime.now())) {
            throw new MessageBusinessException(40071, "scheduledTime must be in the future");
        }

        PrivateMessage row = new PrivateMessage();
        row.setCoupleId(req.coupleId());
        row.setSenderId(senderId);
        row.setReceiverId(req.receiverId());
        row.setContent(req.content().trim());
        row.setMessageType(req.messageType());
        row.setIsScheduled(1);
        row.setScheduledTime(req.scheduledTime());
        row.setIsRead(0);
        row.setReadTime(null);
        row.setIsRetracted(0);
        save(row);
        log.info(
                "message.scheduled id={} coupleId={} senderId={} receiverId={} scheduledTime={}",
                row.getId(),
                row.getCoupleId(),
                senderId,
                req.receiverId(),
                req.scheduledTime());
        return toResponse(row);
    }

    @Override
    public List<PrivateMessageResponse> listMessages(String userId, String coupleId, long page, long pageSize) {
        assertCoupleMember(userId, coupleId);
        long safePage = Math.max(1, page);
        long safePageSize = Math.max(1, Math.min(pageSize, 100));
        LocalDateTime now = LocalDateTime.now();

        Page<PrivateMessage> p = new Page<>(safePage, safePageSize);
        LambdaQueryWrapper<PrivateMessage> w = new LambdaQueryWrapper<>();
        w.eq(PrivateMessage::getCoupleId, coupleId)
                .and(q -> q.eq(PrivateMessage::getSenderId, userId).or().eq(PrivateMessage::getReceiverId, userId))
                .and(q -> q.eq(PrivateMessage::getIsScheduled, 0).or().le(PrivateMessage::getScheduledTime, now))
                .orderByDesc(PrivateMessage::getCreatedAt);
        Page<PrivateMessage> result = page(p, w);
        log.debug(
                "message.list userId={} coupleId={} page={} pageSize={} total={} returned={}",
                userId,
                coupleId,
                safePage,
                safePageSize,
                result.getTotal(),
                result.getRecords().size());
        return result.getRecords().stream().map(MessageServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(String userId, String messageId) {
        PrivateMessage row = getById(messageId);
        if (row == null) {
            throw new MessageBusinessException(40470, "message not found");
        }
        assertCoupleMember(userId, row.getCoupleId());
        if (!userId.equals(row.getReceiverId())) {
            throw new MessageBusinessException(40370, "only receiver can mark read");
        }
        if (Integer.valueOf(1).equals(row.getIsRetracted())) {
            throw new MessageBusinessException(40072, "retracted message cannot be marked as read");
        }
        if (Integer.valueOf(1).equals(row.getIsRead())) {
            return;
        }
        row.setIsRead(1);
        row.setReadTime(LocalDateTime.now());
        updateById(row);
        log.info("message.read id={} receiverId={}", messageId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retract(String userId, String messageId) {
        PrivateMessage row = getById(messageId);
        if (row == null) {
            throw new MessageBusinessException(40470, "message not found");
        }
        assertCoupleMember(userId, row.getCoupleId());
        if (!userId.equals(row.getSenderId())) {
            throw new MessageBusinessException(40371, "only sender can retract");
        }
        if (Integer.valueOf(1).equals(row.getIsRetracted())) {
            return;
        }
        row.setIsRetracted(1);
        updateById(row);
        log.info("message.retracted id={} senderId={}", messageId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int dispatchDueScheduledMessages() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<PrivateMessage> w = new LambdaQueryWrapper<>();
        w.eq(PrivateMessage::getIsScheduled, 1)
                .le(PrivateMessage::getScheduledTime, now)
                .eq(PrivateMessage::getIsRetracted, 0);
        List<PrivateMessage> due = list(w);
        if (due.isEmpty()) {
            return 0;
        }
        for (PrivateMessage row : due) {
            row.setIsScheduled(0);
            updateById(row);
        }
        log.info("message.dispatch.scheduled count={}", due.size());
        return due.size();
    }

    private CoupleBinding assertCoupleMember(String userId, String coupleId) {
        return coupleBindingService
                .findActiveOrFrozenMembership(userId, coupleId)
                .orElseThrow(() -> new MessageBusinessException(40372, "forbidden or invalid couple"));
    }

    private static void assertReceiverIsPartner(String senderId, String receiverId, CoupleBinding membership) {
        if (senderId.equals(receiverId)) {
            throw new MessageBusinessException(40073, "receiver cannot be sender self");
        }
        boolean bothInCouple =
                (senderId.equals(membership.getUserId1()) || senderId.equals(membership.getUserId2()))
                        && (receiverId.equals(membership.getUserId1()) || receiverId.equals(membership.getUserId2()));
        if (!bothInCouple) {
            throw new MessageBusinessException(40074, "receiver must be partner in the same couple");
        }
    }

    private static void validateContentAndType(String content, String messageType) {
        if (isBlank(content)) {
            throw new MessageBusinessException(40075, "content is required");
        }
        if (content.length() > 5000) {
            throw new MessageBusinessException(40076, "content too long");
        }
        if (!"text".equals(messageType) && !"image".equals(messageType) && !"voice".equals(messageType) && !"letter".equals(messageType)) {
            throw new MessageBusinessException(40077, "invalid messageType");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static PrivateMessageResponse toResponse(PrivateMessage m) {
        return new PrivateMessageResponse(
                m.getId(),
                m.getCoupleId(),
                m.getSenderId(),
                m.getReceiverId(),
                m.getContent(),
                m.getMessageType(),
                m.getIsScheduled(),
                m.getScheduledTime(),
                m.getIsRead(),
                m.getReadTime(),
                m.getIsRetracted(),
                m.getCreatedAt());
    }
}
