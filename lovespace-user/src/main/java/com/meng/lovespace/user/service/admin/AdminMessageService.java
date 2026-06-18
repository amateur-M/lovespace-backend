package com.meng.lovespace.user.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.user.entity.PrivateMessage;

/** 管理端私信服务。 */
public interface AdminMessageService {

    IPage<PrivateMessage> page(String coupleId, String senderId, long page, long pageSize);

    void delete(String adminUserId, String messageId);
}
