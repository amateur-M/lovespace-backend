package com.meng.lovespace.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meng.lovespace.user.dto.MessageSendRequest;
import com.meng.lovespace.user.dto.PrivateMessageResponse;
import com.meng.lovespace.user.dto.ScheduledMessageCreateRequest;
import com.meng.lovespace.user.entity.PrivateMessage;
import java.util.List;

/**
 * 私密消息领域服务：发送、列表、已读、撤回、定时消息。
 */
public interface MessageService extends IService<PrivateMessage> {

    PrivateMessageResponse send(String senderId, MessageSendRequest req);

    PrivateMessageResponse createScheduled(String senderId, ScheduledMessageCreateRequest req);

    List<PrivateMessageResponse> listMessages(String userId, String coupleId, long page, long pageSize);

    void markRead(String userId, String messageId);

    void retract(String userId, String messageId);

    int dispatchDueScheduledMessages();
}
