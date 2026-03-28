package com.meng.lovespace.user.config;

import com.meng.lovespace.user.oss.AliyunOssAvatarStorageService;
import com.meng.lovespace.user.oss.AvatarStorageService;
import com.meng.lovespace.user.oss.LocalAvatarStorageService;
import com.meng.lovespace.user.oss.MinioAvatarStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 头像存储策略：优先使用 MinIO，其次阿里云 OSS，最后回退本地磁盘。
 */
@Slf4j
@Configuration
public class AvatarStorageConfig {

    /**
     * @param minioProperties MinIO 配置
     * @param ossProperties 阿里云 OSS
     * @param localStorageProperties 本地上传目录等
     * @return 具体 {@link AvatarStorageService} 实现
     */
    @Bean
    public AvatarStorageService avatarStorageService(
            MinioProperties minioProperties,
            OssProperties ossProperties,
            LocalStorageProperties localStorageProperties) {
        // 优先级 1: MinIO
        if (isMinioConfigured(minioProperties)) {
            log.info("avatar storage: MinIO (bucket={}, endpoint={})", 
                    minioProperties.bucket(), minioProperties.endpoint());
            return new MinioAvatarStorageService(minioProperties);
        }
        
        // 优先级 2: 阿里云 OSS
        if (isOssConfigured(ossProperties)) {
            log.info("avatar storage: Aliyun OSS (bucket={})", ossProperties.bucket());
            return new AliyunOssAvatarStorageService(ossProperties);
        }
        
        // 优先级 3: 本地文件系统
        log.info(
                "avatar storage: local filesystem (uploadDir={})",
                localStorageProperties.uploadDir() == null || localStorageProperties.uploadDir().isBlank()
                        ? "(default uploads)"
                        : localStorageProperties.uploadDir());
        return new LocalAvatarStorageService(localStorageProperties);
    }

    /** 判断 MinIO 必填项是否已配置。 */
    private boolean isMinioConfigured(MinioProperties p) {
        return notBlank(p.endpoint())
                && notBlank(p.accessKey())
                && notBlank(p.secretKey())
                && notBlank(p.bucket());
    }

    /** 判断 OSS 必填项是否已配置。 */
    private boolean isOssConfigured(OssProperties p) {
        return notBlank(p.endpoint())
                && notBlank(p.accessKeyId())
                && notBlank(p.accessKeySecret())
                && notBlank(p.bucket());
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}

