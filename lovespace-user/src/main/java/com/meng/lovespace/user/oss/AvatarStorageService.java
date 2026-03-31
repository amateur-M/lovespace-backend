package com.meng.lovespace.user.oss;

import java.nio.file.Path;
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
     * 上传时间轴媒体（图片或视频）并返回可访问 URL（与头像相同的存储策略，路径前缀为 {@code timeline}）。
     *
     * @param userId 用户 ID
     * @param file 媒体文件
     * @return 访问地址
     */
    String uploadTimelineImage(String userId, MultipartFile file);

    /**
     * 将已合并的本地临时文件发布到时间轴路径（分片上传完成后调用）；成功后调用方可删除临时文件。
     *
     * @param userId 用户 ID
     * @param localFile 磁盘上的临时文件（通常位于 {@code _pending/timeline} 下）
     * @param originalFilename 原始文件名（用于解析扩展名）
     * @param contentType MIME，可为空则按扩展名推断
     * @return 可写入 {@code images_json} 的 URL
     */
    String uploadTimelineFromLocalFile(String userId, Path localFile, String originalFilename, String contentType);

    /**
     * 上传相册照片并返回可访问 URL（与头像相同的存储策略，路径前缀为 {@code albums}）。
     *
     * @param userId 用户 ID
     * @param file 图片文件
     * @return 访问地址
     */
    String uploadAlbumPhoto(String userId, MultipartFile file);
}

