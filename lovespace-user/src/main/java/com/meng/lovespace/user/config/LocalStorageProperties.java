package com.meng.lovespace.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地上传配置，前缀 {@code lovespace.local-storage}。
 *
 * @param uploadDir 磁盘根目录
 * @param publicBaseUrl 返回给前端的 URL 前缀，可为空则使用相对路径 {@code /local-files/...}
 * @param dirPrefix 子目录前缀
 */
@ConfigurationProperties(prefix = "lovespace.local-storage")
public record LocalStorageProperties(String uploadDir, String publicBaseUrl, String dirPrefix) {}

