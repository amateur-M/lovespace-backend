package com.meng.lovespace.user.dto;

import com.meng.lovespace.user.entity.User;

/** 用户实体与对外 DTO 转换。 */
public final class UserDtoMapper {

    private UserDtoMapper() {}

    public static UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getPhone(),
                u.getUsername(),
                u.getEmail(),
                u.getAvatarUrl(),
                u.getGender(),
                u.getBirthday(),
                u.getStatus(),
                u.getRole(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }
}
