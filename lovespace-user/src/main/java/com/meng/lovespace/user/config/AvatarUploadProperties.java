package com.meng.lovespace.user.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 头像上传校验规则，前缀 {@code lovespace.avatar}。
 *
 * @param maxSizeBytes 单文件最大字节数
 * @param allowedExtensions 允许的小写扩展名列表（不含点）
 */
@ConfigurationProperties(prefix = "lovespace.avatar")
public record AvatarUploadProperties(long maxSizeBytes, List<String> allowedExtensions) {}

