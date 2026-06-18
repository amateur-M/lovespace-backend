package com.meng.lovespace.user.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.user.entity.LoveRecord;

/** 管理端时间轴服务。 */
public interface AdminTimelineService {

    IPage<LoveRecord> page(String coupleId, String userId, long page, long pageSize);

    void delete(String adminUserId, String recordId);
}
