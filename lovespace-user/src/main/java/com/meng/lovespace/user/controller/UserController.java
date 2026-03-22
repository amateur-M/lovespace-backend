package com.meng.lovespace.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.dto.UserCreateRequest;
import com.meng.lovespace.user.dto.UserResponse;
import com.meng.lovespace.user.entity.User;
import com.meng.lovespace.user.service.UserService;
import jakarta.validation.Valid;
import java.util.Optional;
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

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param userService 用户服务
     * @param passwordEncoder 密码加密器
     */
    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 创建用户（管理/种子接口），密码经 BCrypt 写入 {@code password_hash}。
     *
     * @param req 用户名、邮箱、密码
     * @return 新建用户信息
     */
    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest req) {
        log.info("users.create username={} email={}", req.username(), req.email());
        boolean exists =
                userService.exists(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getUsername, req.username())
                                .or()
                                .eq(User::getEmail, req.email()));
        if (exists) {
            log.warn("users.create conflict username={} email={}", req.username(), req.email());
            return ApiResponse.error(40001, "username or email already exists");
        }

        User u = new User();
        u.setUsername(req.username());
        u.setEmail(req.email());
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
    public ApiResponse<UserResponse> getById(@PathVariable String id) {
        return Optional.ofNullable(userService.getById(id))
                .map(u -> ApiResponse.ok(toResponse(u)))
                .orElseGet(() -> ApiResponse.error(40400, "user not found"));
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
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        Page<User> p = userService.page(Page.of(page, size));
        IPage<UserResponse> resp = p.convert(this::toResponse);
        return ApiResponse.ok(resp);
    }

    /**
     * 按主键删除用户。
     *
     * @param id 用户 ID
     * @return 成功或 404
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("users.delete id={}", id);
        boolean ok = userService.removeById(id);
        return ok ? ApiResponse.ok() : ApiResponse.error(40400, "user not found");
    }

    /** 实体转 DTO。 */
    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
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

