package com.meng.lovespace.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 对象存储配置，前缀 {@code lovespace.minio}。
 *
 * @param endpoint MinIO 服务端点 URL（如 http://localhost:9000）
 * @param accessKey 访问密钥 ID
 * @param secretKey 访问密钥密码
 * @param bucket 存储桶名称
 * @param publicBaseUrl 可选自定义访问域名/CDN 前缀
 * @param dirPrefix 对象键前缀目录
 */
@ConfigurationProperties(prefix = "lovespace.minio")
public record MinioProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String publicBaseUrl,
        String dirPrefix) {}
