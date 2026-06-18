package com.meng.lovespace.user.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.user.entity.MemorialDay;

/** 管理端纪念日服务。 */
public interface AdminMemorialService {

    IPage<MemorialDay> page(String coupleId, long page, long pageSize);

    void delete(String adminUserId, String id);
}
