package com.meng.lovespace.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meng.lovespace.user.dto.LoginRequest;
import com.meng.lovespace.user.dto.LoginResponse;
import com.meng.lovespace.user.dto.RegisterRequest;
import com.meng.lovespace.user.dto.UserResponse;
import com.meng.lovespace.user.entity.User;
import com.meng.lovespace.user.security.TokenBlacklistService;
import com.meng.lovespace.user.service.AuthService;
import com.meng.lovespace.user.service.UserService;
import com.meng.lovespace.user.util.JwtUtil;
import com.meng.lovespace.user.util.PhoneNormalizer;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * {@link AuthService} 实现：注册加密入库、登录签发 JWT、登出写入 Redis 黑名单。
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklist;

    /**
     * @param userService 用户服务
     * @param passwordEncoder 密码编码器
     * @param jwtUtil JWT 工具
     * @param blacklist token 黑名单
     */
    public AuthServiceImpl(
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            TokenBlacklistService blacklist) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
    }

    /** {@inheritDoc} */
    @Override
    public UserResponse register(RegisterRequest req) {
        String phone = PhoneNormalizer.normalize(req.phone());
        if (!PhoneNormalizer.isValidCnMobile(phone)) {
            throw new IllegalArgumentException("invalid phone number");
        }
        String uname = req.username().trim();
        if (uname.isEmpty()) {
            throw new IllegalArgumentException("username is required");
        }
        boolean exists =
                userService.exists(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getPhone, phone)
                                .or()
                                .eq(User::getUsername, uname));
        if (exists) {
            log.warn("register conflict phone={} username={}", phone, uname);
            throw new IllegalArgumentException("phone or username already exists");
        }

        User u = new User();
        u.setPhone(phone);
        u.setUsername(uname);
        u.setEmail(null);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setStatus(1);
        userService.save(u);
        log.info("register success userId={} phone={}", u.getId(), u.getPhone());
        return toResponse(u);
    }

    /** {@inheritDoc} */
    @Override
    public LoginResponse login(LoginRequest req) {
        String phone = PhoneNormalizer.normalize(req.phone());
        if (!PhoneNormalizer.isValidCnMobile(phone)) {
            throw new IllegalArgumentException("invalid phone number");
        }
        User u =
                userService.getOne(
                        new LambdaQueryWrapper<User>().eq(User::getPhone, phone), false);
        if (u == null || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            log.warn("login failed phone={} reason=bad_credentials", phone);
            throw new IllegalArgumentException("invalid phone or password");
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            log.warn("login failed phone={} reason=user_disabled userId={}", phone, u.getId());
            throw new IllegalArgumentException("user disabled");
        }

        String token = jwtUtil.generateToken(u.getId(), u.getUsername(), u.getPhone());
        log.info("login success userId={} phone={}", u.getId(), u.getPhone());
        return new LoginResponse(token, toResponse(u));
    }

    /** {@inheritDoc} */
    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) return;
        var jws = jwtUtil.parseAndValidate(token);
        var claims = jws.getPayload();
        String jti = jwtUtil.getJti(claims);
        long exp = jwtUtil.getExpiresAtEpochSeconds(claims);
        long now = Instant.now().getEpochSecond();
        Duration ttl = Duration.ofSeconds(Math.max(0, exp - now));
        blacklist.blacklist(jti, ttl);
        log.info("logout blacklist jti={} ttlSeconds={}", jti, ttl.getSeconds());
    }

    /** 领域实体转对外 DTO。 */
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
