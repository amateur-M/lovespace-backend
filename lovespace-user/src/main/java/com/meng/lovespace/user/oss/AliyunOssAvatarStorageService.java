package com.meng.lovespace.user.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.meng.lovespace.user.config.OssProperties;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云 OSS 头像上传实现。
 */
public class AliyunOssAvatarStorageService implements AvatarStorageService {

    private static final Logger log = LoggerFactory.getLogger(AliyunOssAvatarStorageService.class);

    private final OssProperties props;

    /** @param props OSS 连接与桶配置 */
    public AliyunOssAvatarStorageService(OssProperties props) {
        this.props = props;
    }

    /** {@inheritDoc} */
    @Override
    public String uploadAvatar(String userId, MultipartFile file) {
        validateConfig();

        String ext = getExtension(file.getOriginalFilename());
        String prefix = props.dirPrefix() == null || props.dirPrefix().isBlank() ? "avatars" : props.dirPrefix();
        String objectKey =
                "%s/%s/%s/%s-%s.%s"
                        .formatted(
                                prefix,
                                LocalDate.now(),
                                userId,
                                System.currentTimeMillis(),
                                UUID.randomUUID().toString().substring(0, 8),
                                ext);

        OSS ossClient =
                new OSSClientBuilder().build(props.endpoint(), props.accessKeyId(), props.accessKeySecret());
        try (InputStream in = file.getInputStream()) {
            ossClient.putObject(props.bucket(), objectKey, in);
        } catch (IOException e) {
            throw new IllegalStateException("upload avatar failed", e);
        } finally {
            ossClient.shutdown();
        }

        String url;
        if (props.publicBaseUrl() != null && !props.publicBaseUrl().isBlank()) {
            url = trimRightSlash(props.publicBaseUrl()) + "/" + objectKey;
        } else {
            url = "https://%s.%s/%s".formatted(props.bucket(), stripHttp(props.endpoint()), objectKey);
        }
        log.info("oss avatar uploaded userId={} objectKey={} url={}", userId, objectKey, url);
        return url;
    }

    @Override
    public String uploadTimelineImage(String userId, MultipartFile file) {
        validateConfig();

        String ext = getExtension(file.getOriginalFilename());
        String objectKey =
                "timeline/%s/%s/%s-%s.%s"
                        .formatted(
                                LocalDate.now(),
                                userId,
                                System.currentTimeMillis(),
                                UUID.randomUUID().toString().substring(0, 8),
                                ext);

        OSS ossClient =
                new OSSClientBuilder().build(props.endpoint(), props.accessKeyId(), props.accessKeySecret());
        try (InputStream in = file.getInputStream()) {
            ossClient.putObject(props.bucket(), objectKey, in);
        } catch (IOException e) {
            throw new IllegalStateException("upload timeline image failed", e);
        } finally {
            ossClient.shutdown();
        }

        String url;
        if (props.publicBaseUrl() != null && !props.publicBaseUrl().isBlank()) {
            url = trimRightSlash(props.publicBaseUrl()) + "/" + objectKey;
        } else {
            url = "https://%s.%s/%s".formatted(props.bucket(), stripHttp(props.endpoint()), objectKey);
        }
        log.info("oss timeline image uploaded userId={} objectKey={} url={}", userId, objectKey, url);
        return url;
    }

    @Override
    public String uploadAlbumPhoto(String userId, MultipartFile file) {
        validateConfig();

        String ext = getExtension(file.getOriginalFilename());
        String objectKey =
                "albums/%s/%s/%s-%s.%s"
                        .formatted(
                                LocalDate.now(),
                                userId,
                                System.currentTimeMillis(),
                                UUID.randomUUID().toString().substring(0, 8),
                                ext);

        OSS ossClient =
                new OSSClientBuilder().build(props.endpoint(), props.accessKeyId(), props.accessKeySecret());
        try (InputStream in = file.getInputStream()) {
            ossClient.putObject(props.bucket(), objectKey, in);
        } catch (IOException e) {
            throw new IllegalStateException("upload album image failed", e);
        } finally {
            ossClient.shutdown();
        }

        String url;
        if (props.publicBaseUrl() != null && !props.publicBaseUrl().isBlank()) {
            url = trimRightSlash(props.publicBaseUrl()) + "/" + objectKey;
        } else {
            url = "https://%s.%s/%s".formatted(props.bucket(), stripHttp(props.endpoint()), objectKey);
        }
        log.info("oss album image uploaded userId={} objectKey={} url={}", userId, objectKey, url);
        return url;
    }

    /** 校验 endpoint、AK、桶等必填项。 */
    private void validateConfig() {
        if (isBlank(props.endpoint())
                || isBlank(props.accessKeyId())
                || isBlank(props.accessKeySecret())
                || isBlank(props.bucket())) {
            throw new IllegalStateException("OSS config incomplete: endpoint/accessKeyId/accessKeySecret/bucket");
        }
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

