package com.meng.lovespace.user.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.entity.PrivateMessage;
import com.meng.lovespace.user.mapper.MessageMapper;
import com.meng.lovespace.user.service.admin.AdminMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AdminMessageServiceImpl implements AdminMessageService {

    private final MessageMapper messageMapper;

    public AdminMessageServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public IPage<PrivateMessage> page(String coupleId, String senderId, long page, long pageSize) {
        LambdaQueryWrapper<PrivateMessage> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(coupleId)) {
            qw.eq(PrivateMessage::getCoupleId, coupleId.trim());
        }
        if (StringUtils.hasText(senderId)) {
            qw.eq(PrivateMessage::getSenderId, senderId.trim());
        }
        qw.orderByDesc(PrivateMessage::getCreatedAt);
        return messageMapper.selectPage(Page.of(page, pageSize), qw);
    }

    @Override
    public void delete(String adminUserId, String messageId) {
        if (!messageMapper.exists(
                new LambdaQueryWrapper<PrivateMessage>().eq(PrivateMessage::getId, messageId))) {
            throw new ApiBusinessException(40400, "message not found");
        }
        messageMapper.deleteById(messageId);
        log.info("admin.messages.delete adminUserId={} messageId={}", adminUserId, messageId);
    }
}
