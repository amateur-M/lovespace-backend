package com.meng.lovespace.user.controller;

import com.meng.lovespace.common.web.ApiResponse;
import com.meng.lovespace.user.config.AvatarUploadProperties;
import com.meng.lovespace.user.dto.UpdateProfileRequest;
import com.meng.lovespace.user.dto.UserResponse;
import com.meng.lovespace.user.entity.User;
import com.meng.lovespace.user.oss.AvatarStorageService;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 当前登录用户的个人资料接口：查询、更新、头像上传。
 *
 * <p>需携带有效 JWT；头像上传受 {@link AvatarUploadProperties} 限制类型与大小。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user/profile")
public class ProfileController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    private final UserService userService;
    private final AvatarStorageService avatarStorageService;
    private final AvatarUploadProperties avatarUploadProperties;

    /**
     * @param userService 用户数据服务
     * @param avatarStorageService OSS 或本地存储实现
     * @param avatarUploadProperties 上传校验配置
     */
    public ProfileController(
            UserService userService,
            AvatarStorageService avatarStorageService,
            AvatarUploadProperties avatarUploadProperties) {
        this.userService = userService;
        this.avatarStorageService = avatarStorageService;
        this.avatarUploadProperties = avatarUploadProperties;
    }

    /**
     * 获取当前登录用户资料。
     *
     * @param auth Spring Security 认证对象，principal 为 {@link JwtUserPrincipal}
     * @return 用户信息或 404
     */
    @GetMapping
    public ApiResponse<UserResponse> getProfile(Authentication auth) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.debug("profile.get userId={}", p.userId());
        return Optional.ofNullable(userService.getById(p.userId()))
                .map(u -> ApiResponse.ok(toResponse(u)))
                .orElseGet(() -> ApiResponse.error(40400, "user not found"));
    }

    /**
     * 更新可编辑字段（头像 URL、性别、生日、用户名、邮箱）；未传的字段不修改；邮箱传空串表示清空。
     *
     * @param auth 当前用户
     * @param req 更新请求体
     * @return 更新后的用户信息
     */
    @PutMapping
    public ApiResponse<UserResponse> updateProfile(
            Authentication auth, @Valid @RequestBody UpdateProfileRequest req) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        log.info(
                "profile.update userId={} avatarUrlSet={} genderSet={} birthdaySet={} usernameSet={} emailSet={}",
                p.userId(),
                req.avatarUrl() != null,
                req.gender() != null,
                req.birthday() != null,
                req.username() != null,
                req.email() != null);
        User u = userService.getById(p.userId());
        if (u == null) {
            log.warn("profile.update user not found userId={}", p.userId());
            return ApiResponse.error(40400, "user not found");
        }

        if (req.avatarUrl() != null) u.setAvatarUrl(req.avatarUrl());
        if (req.gender() != null) u.setGender(req.gender());
        if (req.birthday() != null) u.setBirthday(req.birthday());

        if (req.username() != null && !req.username().isBlank()) {
            String nu = req.username().trim();
            if (!nu.equals(u.getUsername())) {
                if (userService.existsUsernameForOtherUser(nu, u.getId())) {
                    return ApiResponse.error(40001, "username already taken");
                }
                u.setUsername(nu);
            }
        }
        if (req.email() != null) {
            String raw = req.email().trim();
            if (raw.isEmpty()) {
                u.setEmail(null);
            } else {
                if (!EMAIL_PATTERN.matcher(raw).matches()) {
                    return ApiResponse.error(40001, "invalid email format");
                }
                if (userService.existsEmailForOtherUser(raw, u.getId())) {
                    return ApiResponse.error(40001, "email already taken");
                }
                u.setEmail(raw);
            }
        }

        userService.updateById(u);
        return ApiResponse.ok(toResponse(u));
    }

    /**
     * 上传头像：校验后写入存储（OSS 或本地），并更新用户 {@code avatar_url}。
     *
     * @param auth 当前用户
     * @param file multipart 字段名 {@code file}
     * @return 头像可访问 URL
     */
    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(Authentication auth, @RequestPart("file") MultipartFile file) {
        JwtUserPrincipal p = (JwtUserPrincipal) auth.getPrincipal();
        if (file == null || file.isEmpty()) {
            log.warn("profile.avatar upload rejected userId={} reason=empty", p.userId());
            return ApiResponse.error(40010, "avatar file is required");
        }
        if (file.getSize() > avatarUploadProperties.maxSizeBytes()) {
            log.warn(
                    "profile.avatar upload rejected userId={} size={} maxBytes={}",
                    p.userId(),
                    file.getSize(),
                    avatarUploadProperties.maxSizeBytes());
            return ApiResponse.error(
                    40011,
                    "avatar too large, max "
                            + avatarUploadProperties.maxSizeBytes() / 1024 / 1024
                            + "MB");
        }
        String ext = getExtension(file.getOriginalFilename());
        List<String> allowed = avatarUploadProperties.allowedExtensions();
        if (allowed == null || allowed.isEmpty()) {
            allowed = List.of("jpg", "jpeg", "png", "webp");
        }
        if (!allowed.stream().map(String::toLowerCase).toList().contains(ext)) {
            log.warn(
                    "profile.avatar upload rejected userId={} ext={} allowed={}",
                    p.userId(),
                    ext,
                    allowed);
            return ApiResponse.error(40012, "invalid avatar type, allowed: " + String.join(",", allowed));
        }

        log.info(
                "profile.avatar upload start userId={} originalName={} size={} contentType={}",
                p.userId(),
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType());
        String url = avatarStorageService.uploadAvatar(p.userId(), file);
        User u = userService.getById(p.userId());
        if (u == null) {
            log.warn("profile.avatar user not found after upload userId={}", p.userId());
            return ApiResponse.error(40400, "user not found");
        }
        u.setAvatarUrl(url);
        userService.updateById(u);
        log.info("profile.avatar upload done userId={} url={}", p.userId(), url);
        return ApiResponse.ok(url);
    }

    /** 实体转对外 DTO（不含密码）。 */
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

    /** 从原始文件名解析小写扩展名，无点则返回空串。 */
    private static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}

