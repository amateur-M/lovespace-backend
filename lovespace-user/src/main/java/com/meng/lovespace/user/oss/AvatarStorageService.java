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

    /**
     * 上传时间轴配图并返回可访问 URL（与头像相同的存储策略，路径前缀为 {@code timeline}）。
     *
     * @param userId 用户 ID
     * @param file 图片文件
     * @return 访问地址
     */
    String uploadTimelineImage(String userId, MultipartFile file);

    /**
     * 上传相册照片并返回可访问 URL（与头像相同的存储策略，路径前缀为 {@code albums}）。
     *
     * @param userId 用户 ID
     * @param file 图片文件
     * @return 访问地址
     */
    String uploadAlbumPhoto(String userId, MultipartFile file);
}

