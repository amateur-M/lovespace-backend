package com.meng.lovespace.user.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.dto.UserCreateRequest;
import com.meng.lovespace.user.dto.UserDtoMapper;
import com.meng.lovespace.user.dto.UserResponse;
import com.meng.lovespace.user.dto.admin.AdminUserUpdateRequest;
import com.meng.lovespace.user.entity.User;
import com.meng.lovespace.user.service.UserService;
import com.meng.lovespace.user.service.admin.AdminUserService;
import com.meng.lovespace.user.user.UserRole;
import com.meng.lovespace.user.util.PhoneNormalizer;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AdminUserServiceImpl(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse create(UserCreateRequest req) {
        String phone = PhoneNormalizer.normalize(req.phone());
        if (!PhoneNormalizer.isValidCnMobile(phone)) {
            throw new ApiBusinessException(40001, "invalid phone number");
        }
        String uname = req.username().trim();
        String email =
                req.email() != null && !req.email().isBlank() ? req.email().trim() : null;
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ApiBusinessException(40001, "invalid email format");
        }
        LambdaQueryWrapper<User> conflict = new LambdaQueryWrapper<>();
        conflict.eq(User::getUsername, uname).or().eq(User::getPhone, phone);
        if (email != null) {
            conflict.or().eq(User::getEmail, email);
        }
        if (userService.exists(conflict)) {
            throw new ApiBusinessException(40001, "username, phone or email already exists");
        }
        User u = new User();
        u.setPhone(phone);
        u.setUsername(uname);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setStatus(1);
        u.setRole(UserRole.USER.code());
        userService.save(u);
        log.info("admin.users.create userId={} phone={}", u.getId(), phone);
        return UserDtoMapper.toResponse(u);
    }

    @Override
    public IPage<UserResponse> page(String keyword, long page, long pageSize) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            qw.and(
                    w ->
                            w.like(User::getPhone, kw)
                                    .or()
                                    .like(User::getUsername, kw)
                                    .or()
                                    .like(User::getEmail, kw));
        }
        qw.orderByDesc(User::getCreatedAt);
        return userService.page(Page.of(page, pageSize), qw).convert(UserDtoMapper::toResponse);
    }

    @Override
    public UserResponse getById(String id) {
        User u = userService.getById(id);
        if (u == null) {
            throw new ApiBusinessException(40400, "user not found");
        }
        return UserDtoMapper.toResponse(u);
    }

    @Override
    public UserResponse update(String adminUserId, String id, AdminUserUpdateRequest req) {
        User u = userService.getById(id);
        if (u == null) {
            throw new ApiBusinessException(40400, "user not found");
        }
        if (req.status() != null) {
            u.setStatus(req.status());
        }
        if (req.role() != null) {
            int newRole = req.role();
            if (UserRole.fromCode(u.getRole()).isAdmin()
                    && newRole == UserRole.USER.code()
                    && id.equals(adminUserId)) {
                ensureNotLastAdmin(id);
            }
            if (UserRole.fromCode(u.getRole()).isAdmin() && newRole == UserRole.USER.code()) {
                ensureNotLastAdmin(id);
            }
            u.setRole(newRole);
        }
        userService.updateById(u);
        log.info("admin.users.update adminUserId={} targetUserId={}", adminUserId, id);
        return UserDtoMapper.toResponse(u);
    }

    @Override
    public void delete(String adminUserId, String id) {
        if (id.equals(adminUserId)) {
            throw new ApiBusinessException(40001, "cannot delete yourself");
        }
        User u = userService.getById(id);
        if (u == null) {
            throw new ApiBusinessException(40400, "user not found");
        }
        if (UserRole.fromCode(u.getRole()).isAdmin()) {
            ensureNotLastAdmin(id);
        }
        if (!userService.removeById(id)) {
            throw new ApiBusinessException(40400, "user not found");
        }
        log.info("admin.users.delete adminUserId={} targetUserId={}", adminUserId, id);
    }

    @Override
    public boolean verifyPassword(String id, String password) {
        User user = userService.getById(id);
        if (user == null) {
            throw new ApiBusinessException(40400, "user not found");
        }
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    private void ensureNotLastAdmin(String targetUserId) {
        long adminCount =
                userService.count(
                        new LambdaQueryWrapper<User>().eq(User::getRole, UserRole.ADMIN.code()));
        if (adminCount <= 1) {
            User target = userService.getById(targetUserId);
            if (target != null && UserRole.fromCode(target.getRole()).isAdmin()) {
                throw new ApiBusinessException(40001, "cannot remove the last admin");
            }
        }
    }
}
