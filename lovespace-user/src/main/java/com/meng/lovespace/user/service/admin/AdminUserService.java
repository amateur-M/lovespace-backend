package com.meng.lovespace.user.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.user.dto.UserCreateRequest;
import com.meng.lovespace.user.dto.UserResponse;
import com.meng.lovespace.user.dto.admin.AdminDashboardStatsResponse;
import com.meng.lovespace.user.dto.admin.AdminUserUpdateRequest;

/** 管理端用户服务。 */
public interface AdminUserService {

    UserResponse create(UserCreateRequest req);

    IPage<UserResponse> page(String keyword, long page, long pageSize);

    UserResponse getById(String id);

    UserResponse update(String adminUserId, String id, AdminUserUpdateRequest req);

    void delete(String adminUserId, String id);

    boolean verifyPassword(String id, String password);
}
