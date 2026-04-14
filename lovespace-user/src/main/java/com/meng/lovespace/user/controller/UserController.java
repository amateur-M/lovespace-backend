package com.meng.lovespace.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.PasswordVerifyRequest;
import com.meng.lovespace.user.dto.UserCreateRequest;
import com.meng.lovespace.user.dto.UserResponse;
import com.meng.lovespace.user.entity.User;
import com.meng.lovespace.user.service.UserService;
import com.meng.lovespace.user.util.PhoneNormalizer;
import jakarta.validation.Valid;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理 REST（创建、查询、分页、删除）。
 *
 * <p>路径前缀 {@code /users}，与认证后的个人资料接口分离；部分操作需登录视安全配置而定。
 */
@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param userService 用户服务
     * @param passwordEncoder 密码加密器
     */
    /** @param userService 用户持久化服务 @param passwordEncoder 注册时密码哈希 */
    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 创建用户（管理/种子接口），密码经 BCrypt 写入 {@code password_hash}。
     *
     * @param req 手机号、用户名、可选邮箱、密码
     * @return 新建用户信息
     */
    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest req) {
        String phone = PhoneNormalizer.normalize(req.phone());
        if (!PhoneNormalizer.isValidCnMobile(phone)) {
            return ApiResponse.error(40001, "invalid phone number");
        }
        String uname = req.username().trim();
        String email =
                req.email() != null && !req.email().isBlank() ? req.email().trim() : null;
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            return ApiResponse.error(40001, "invalid email format");
        }
        log.info("users.create phone={} username={}", phone, uname);
        LambdaQueryWrapper<User> conflict = new LambdaQueryWrapper<User>();
        conflict.eq(User::getUsername, uname).or().eq(User::getPhone, phone);
        if (email != null) {
            conflict.or().eq(User::getEmail, email);
        }
        boolean exists = userService.exists(conflict);
        if (exists) {
            log.warn("users.create conflict phone={} username={}", phone, uname);
            return ApiResponse.error(40001, "username, phone or email already exists");
        }

        User u = new User();
        u.setPhone(phone);
        u.setUsername(uname);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setStatus(1);
        userService.save(u);
        log.info("users.create success userId={}", u.getId());
        return ApiResponse.ok(toResponse(u));
    }

    /**
     * 按主键查询用户。
     *
     * @param id 用户 ID
     * @return 用户或 404
     */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable("id") String id) {
        User u = userService.getById(id);
        if (u == null) {
            log.debug("users.getById notFound id={}", id);
            return ApiResponse.error(40400, "user not found");
        }
        log.debug("users.getById id={} username={}", id, u.getUsername());
        return ApiResponse.ok(toResponse(u));
    }

    /**
     * 分页列表。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 分页数据
     */
    @GetMapping
    public ApiResponse<IPage<UserResponse>> page(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        log.debug("users.page page={} size={}", page, size);
        Page<User> p = userService.page(Page.of(page, size));
        IPage<UserResponse> resp = p.convert(this::toResponse);
        log.debug("users.page total={} currentSize={}", resp.getTotal(), resp.getRecords().size());
        return ApiResponse.ok(resp);
    }

    /**
     * 验证用户密码(BCrypt 为单向哈希,无法解密,仅支持验证)。
     *
     * @param userId 用户 ID
     * @param request 包含待验证的明文密码的请求体
     * @return 验证结果
     */
    @PostMapping("/{id}/verify-password")
    public ApiResponse<Boolean> verifyPassword(
            @PathVariable("id") String userId,
            @RequestBody PasswordVerifyRequest request) {
        log.info("users.verifyPassword userId={}", userId);
        User user = userService.getById(userId);
        if (user == null) {
            log.warn("users.verifyPassword notFound userId={}", userId);
            return ApiResponse.error(40400, "user not found");
        }
        boolean matches = passwordEncoder.matches(request.password(), user.getPasswordHash());
        log.info("users.verifyPassword userId={} result={}", userId, matches);
        return ApiResponse.ok(matches);
    }

    /**
     * 按主键删除用户。
     *
     * @param id 用户 ID
     * @return 成功或 404
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") String id) {
        log.info("users.delete id={}", id);
        boolean ok = userService.removeById(id);
        if (!ok) {
            log.debug("users.delete notFound id={}", id);
        }
        return ok ? ApiResponse.ok() : ApiResponse.error(40400, "user not found");
    }

    /** 实体转 DTO。 */
    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getPhone(),
                u.getUsername(),
                u.getEmail(),
                u.getAvatarUrl(),
                u.getGender(),
                u.getBirthday(),
                u.getStatus(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }
}

