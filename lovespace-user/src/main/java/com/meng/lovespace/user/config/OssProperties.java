package com.meng.lovespace.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 阿里云 OSS 配置，前缀 {@code lovespace.oss}。
 *
 * @param endpoint 访问域名
 * @param accessKeyId AccessKey ID
 * @param accessKeySecret AccessKey Secret
 * @param bucket 存储桶
 * @param publicBaseUrl 可选自定义访问域名/CDN
 * @param dirPrefix 对象键前缀目录
 */
@ConfigurationProperties(prefix = "lovespace.oss")
public record OssProperties(
        String endpoint,
        String accessKeyId,
        String accessKeySecret,
        String bucket,
        String publicBaseUrl,
        String dirPrefix) {}

