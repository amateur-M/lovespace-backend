package com.meng.lovespace.user.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meng.lovespace.user.config.LovespaceAdminProperties;
import com.meng.lovespace.user.entity.User;
import com.meng.lovespace.user.service.UserService;
import com.meng.lovespace.user.user.UserRole;
import com.meng.lovespace.user.util.PhoneNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 启动时按配置将指定手机号用户提升为首个管理员。 */
@Slf4j
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final LovespaceAdminProperties adminProperties;
    private final UserService userService;

    public AdminBootstrapRunner(LovespaceAdminProperties adminProperties, UserService userService) {
        this.adminProperties = adminProperties;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String raw = adminProperties.bootstrapPhone();
        if (raw == null || raw.isBlank()) {
            return;
        }
        long adminCount =
                userService.count(
                        new LambdaQueryWrapper<User>().eq(User::getRole, UserRole.ADMIN.code()));
        if (adminCount > 0) {
            return;
        }
        String phone = PhoneNormalizer.normalize(raw);
        User u =
                userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone), false);
        if (u == null) {
            log.warn("admin bootstrap skipped: user not found phone={}", phone);
            return;
        }
        u.setRole(UserRole.ADMIN.code());
        userService.updateById(u);
        log.info("admin bootstrap promoted userId={} phone={}", u.getId(), phone);
    }
}
