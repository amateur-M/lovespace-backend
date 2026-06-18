package com.meng.lovespace.user.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.user.dto.admin.AdminCoupleListItem;

/** 管理端情侣绑定服务。 */
public interface AdminCoupleService {

    IPage<AdminCoupleListItem> page(Integer status, String keyword, long page, long pageSize);

    AdminCoupleListItem getById(String id);

    void forceSeparate(String adminUserId, String coupleId);
}
