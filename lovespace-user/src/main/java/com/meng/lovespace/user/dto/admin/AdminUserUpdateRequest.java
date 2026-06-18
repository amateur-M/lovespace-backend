package com.meng.lovespace.user.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 管理端更新用户状态与角色。
 *
 * @param status 账号状态（1 正常 0 禁用），null 不修改
 * @param role 角色（0 USER 1 ADMIN），null 不修改
 */
public record AdminUserUpdateRequest(
        @Min(0) @Max(1) Integer status, @Min(0) @Max(1) Integer role) {}
