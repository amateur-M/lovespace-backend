package com.meng.lovespace.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.meng.lovespace.user.entity.User;
import java.util.Optional;

/**
 * 用户业务服务，基于 MyBatis-Plus {@link IService} 提供 CRUD 与分页等能力。
 */
public interface UserService extends IService<User> {

    /** 按归一化后的手机号查询用户。 */
    default Optional<User> findByNormalizedPhone(String normalizedPhone) {
        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, normalizedPhone), false));
    }

    /** 是否存在其他用户占用该用户名。 */
    default boolean existsUsernameForOtherUser(String username, String excludeUserId) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return lambdaQuery()
                .eq(User::getUsername, username.trim())
                .ne(User::getId, excludeUserId)
                .exists();
    }

    /** 是否存在其他用户占用该邮箱（email 非空时）。 */
    default boolean existsEmailForOtherUser(String email, String excludeUserId) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return lambdaQuery()
                .eq(User::getEmail, email.trim())
                .ne(User::getId, excludeUserId)
                .exists();
    }
}
