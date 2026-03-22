package com.meng.lovespace.user.oss;

import com.meng.lovespace.user.config.LocalStorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

/**
 * 本地上传头像：写入 {@link LocalStorageProperties#uploadDir()} 下，并返回 {@code /local-files/...} 或自定义公网前缀。
 */
public class LocalAvatarStorageService implements AvatarStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalAvatarStorageService.class);

    private final LocalStorageProperties props;

    /** @param props 本地上传目录与 URL 前缀 */
    public LocalAvatarStorageService(LocalStorageProperties props) {
        this.props = props;
    }

    /** {@inheritDoc} */
    @Override
    public String uploadAvatar(String userId, MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        String prefix =
                props.dirPrefix() == null || props.dirPrefix().isBlank()
                        ? "avatars"
                        : props.dirPrefix();
        String objectKey =
                "%s/%s/%s/%s-%s.%s"
                        .formatted(
                                prefix,
                                LocalDate.now(),
                                userId,
                                System.currentTimeMillis(),
                                UUID.randomUUID().toString().substring(0, 8),
                                ext);

        Path root = resolveRootDir();
        Path target = root.resolve(objectKey).normalize();
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new IllegalStateException("save avatar to local storage failed", e);
        }

        log.info(
                "local avatar saved userId={} absolutePath={} size={}",
                userId,
                target,
                file.getSize());

        String base = props.publicBaseUrl();
        if (base == null || base.isBlank()) {
            return "/local-files/" + objectKey;
        }
        return trimRightSlash(base) + "/" + objectKey;
    }

    /** 解析上传根目录，未配置则使用当前工作目录下 {@code uploads}。 */
    private Path resolveRootDir() {
        String configured = props.uploadDir();
        if (configured == null || configured.isBlank()) {
            return Paths.get("uploads").toAbsolutePath();
        }
        return Paths.get(configured).toAbsolutePath();
    }

    /** 无扩展名时默认 {@code jpg}。 */
    private static String getExtension(String name) {
        if (name == null || !name.contains(".")) return "jpg";
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }

    private static String trimRightSlash(String s) {
        String v = s.trim();
        while (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        return v;
    }
}

