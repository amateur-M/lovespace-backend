package com.meng.lovespace.user.oss;

import com.meng.lovespace.user.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

/**
 * MinIO 对象存储实现。
 */
public class MinioAvatarStorageService implements AvatarStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioAvatarStorageService.class);

    private final MinioProperties props;
    private final MinioClient minioClient;

    /**
     * @param props MinIO 配置属性
     */
    public MinioAvatarStorageService(MinioProperties props) {
        this.props = props;
        this.minioClient = MinioClient.builder()
                .endpoint(props.endpoint())
                .credentials(props.accessKey(), props.secretKey())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public String uploadAvatar(String userId, MultipartFile file) {
        validateConfig();
        ensureBucketExists();

        String ext = getExtension(file.getOriginalFilename());
        String prefix = props.dirPrefix() == null || props.dirPrefix().isBlank() ? "avatars" : props.dirPrefix();
        String objectKey = "%s/%s/%s/%s-%s.%s".formatted(
                prefix,
                LocalDate.now(),
                userId,
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8),
                ext);

        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("upload avatar to MinIO failed", e);
        }

        String url = buildUrl(objectKey);
        log.info("MinIO avatar uploaded userId={} objectKey={} url={}", userId, objectKey, url);
        return url;
    }

    @Override
    public String uploadTimelineImage(String userId, MultipartFile file) {
        validateConfig();
        ensureBucketExists();

        String ext = getExtension(file.getOriginalFilename());
        String objectKey = "timeline/%s/%s/%s-%s.%s".formatted(
                LocalDate.now(),
                userId,
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8),
                ext);

        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("upload timeline image to MinIO failed", e);
        }

        String url = buildUrl(objectKey);
        log.info("MinIO timeline image uploaded userId={} objectKey={} url={}", userId, objectKey, url);
        return url;
    }

    @Override
    public String uploadTimelineFromLocalFile(String userId, Path localFile, String originalFilename, String contentType) {
        validateConfig();
        ensureBucketExists();

        String ext = getExtension(originalFilename);
        String objectKey = "timeline/%s/%s/%s-%s.%s".formatted(
                LocalDate.now(),
                userId,
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8),
                ext);

        String ct = contentType == null || contentType.isBlank() ? guessContentType(originalFilename) : contentType;
        long size;
        try {
            size = Files.size(localFile);
        } catch (Exception e) {
            throw new IllegalStateException("read timeline temp file size failed", e);
        }

        try (InputStream in = Files.newInputStream(localFile)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(objectKey)
                    .stream(in, size, -1)
                    .contentType(ct)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("upload timeline file to MinIO failed", e);
        }

        String url = buildUrl(objectKey);
        log.info("MinIO timeline file from path userId={} objectKey={} url={}", userId, objectKey, url);
        return url;
    }

    private static String guessContentType(String name) {
        String g = java.net.URLConnection.guessContentTypeFromName(name);
        return g != null ? g : "application/octet-stream";
    }

    @Override
    public String uploadAlbumPhoto(String userId, MultipartFile file) {
        validateConfig();
        ensureBucketExists();

        String ext = getExtension(file.getOriginalFilename());
        String objectKey = "albums/%s/%s/%s-%s.%s".formatted(
                LocalDate.now(),
                userId,
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8),
                ext);

        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("upload album image to MinIO failed", e);
        }

        String url = buildUrl(objectKey);
        log.info("MinIO album image uploaded userId={} objectKey={} url={}", userId, objectKey, url);
        return url;
    }

    /** 校验 MinIO 必填项。 */
    private void validateConfig() {
        if (isBlank(props.endpoint())
                || isBlank(props.accessKey())
                || isBlank(props.secretKey())
                || isBlank(props.bucket())) {
            throw new IllegalStateException("MinIO config incomplete: endpoint/accessKey/secretKey/bucket");
        }
    }

    /** 确保存储桶存在，不存在则创建。 */
    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(props.bucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(props.bucket()).build());
                log.info("MinIO bucket created: {}", props.bucket());
            }
        } catch (Exception e) {
            throw new IllegalStateException("failed to check or create MinIO bucket", e);
        }
    }

    /** 构建访问 URL。 */
    private String buildUrl(String objectKey) {
        if (props.publicBaseUrl() != null && !props.publicBaseUrl().isBlank()) {
            return trimRightSlash(props.publicBaseUrl()) + "/" + objectKey;
        }
        // 使用 MinIO endpoint 构建 URL
        String endpoint = stripHttp(props.endpoint());
        return "http://%s/%s/%s".formatted(endpoint, props.bucket(), objectKey);
    }

    private static String getExtension(String name) {
        if (name == null || !name.contains(".")) return "jpg";
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimRightSlash(String s) {
        String v = s.trim();
        while (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        return v;
    }

    private static String stripHttp(String endpoint) {
        String v = endpoint.trim();
        if (v.startsWith("https://")) return v.substring("https://".length());
        if (v.startsWith("http://")) return v.substring("http://".length());
        return v;
    }
}
