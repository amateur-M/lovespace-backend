package com.meng.lovespace.user.config;

import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 将本地上传目录映射为 {@code /local-files/**} 静态资源，供浏览器访问头像等文件。
 */
@Configuration
public class WebStaticResourceConfig implements WebMvcConfigurer {

    private final LocalStorageProperties localStorageProperties;

    /** @param localStorageProperties 本地上传根目录配置 */
    public WebStaticResourceConfig(LocalStorageProperties localStorageProperties) {
        this.localStorageProperties = localStorageProperties;
    }

    /** {@inheritDoc} */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = localStorageProperties.uploadDir();
        if (uploadDir == null || uploadDir.isBlank()) {
            uploadDir = "uploads";
        }
        String absolute = Paths.get(uploadDir).toAbsolutePath().toString().replace("\\", "/");
        if (!absolute.endsWith("/")) absolute += "/";
        registry.addResourceHandler("/local-files/**").addResourceLocations("file:" + absolute);
    }
}

