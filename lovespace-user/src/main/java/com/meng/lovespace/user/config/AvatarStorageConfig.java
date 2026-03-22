package com.meng.lovespace.user.config;

import com.meng.lovespace.user.oss.AliyunOssAvatarStorageService;
import com.meng.lovespace.user.oss.AvatarStorageService;
import com.meng.lovespace.user.oss.LocalAvatarStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 头像存储策略：OSS 配置齐全时使用阿里云 OSS，否则回退本地磁盘。
 */
@Slf4j
@Configuration
public class AvatarStorageConfig {

    /**
     * @param ossProperties 阿里云 OSS
     * @param localStorageProperties 本地上传目录等
     * @return 具体 {@link AvatarStorageService} 实现
     */
    @Bean
    public AvatarStorageService avatarStorageService(
            OssProperties ossProperties, LocalStorageProperties localStorageProperties) {
        if (isConfigured(ossProperties)) {
            log.info("avatar storage: Aliyun OSS (bucket={})", ossProperties.bucket());
            return new AliyunOssAvatarStorageService(ossProperties);
        }
        log.info(
                "avatar storage: local filesystem (uploadDir={})",
                localStorageProperties.uploadDir() == null || localStorageProperties.uploadDir().isBlank()
                        ? "(default uploads)"
                        : localStorageProperties.uploadDir());
        return new LocalAvatarStorageService(localStorageProperties);
    }

    /** 判断 OSS 必填项是否已配置。 */
    private boolean isConfigured(OssProperties p) {
        return notBlank(p.endpoint())
                && notBlank(p.accessKeyId())
                && notBlank(p.accessKeySecret())
                && notBlank(p.bucket());
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}

