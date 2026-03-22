package com.meng.lovespace.user.oss;

import org.springframework.web.multipart.MultipartFile;

/**
 * 头像对象存储抽象：可由 OSS 或本地实现。
 */
public interface AvatarStorageService {

    /**
     * 上传用户头像并返回可访问 URL（绝对或站点相对路径）。
     *
     * @param userId 用户 ID（用于路径隔离）
     * @param file 上传文件
     * @return 头像访问地址
     */
    String uploadAvatar(String userId, MultipartFile file);
}

