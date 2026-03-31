package com.meng.lovespace.user.controller;

import com.meng.lovespace.user.config.LocalStorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本地上传文件的 HTTP 访问入口，显式支持 {@code Range}（视频拖拽进度、分段缓冲）。
 *
 * <p>替代 {@code ResourceHandlerRegistry} 的 file: 映射，保证返回 {@code Accept-Ranges: bytes} 与 {@code 206 Partial Content}。
 */
@RestController
public class LocalFileRangeController {

    private final LocalStorageProperties localStorageProperties;

    /** @param localStorageProperties 本地上传根目录 */
    public LocalFileRangeController(LocalStorageProperties localStorageProperties) {
        this.localStorageProperties = localStorageProperties;
    }

    /**
     * 读取 {@code /local-files/**} 下文件；支持单段 {@code Range} 请求。
     *
     * @param filepath 相对上传根目录的路径（不含 {@code local-files} 前缀）
     * @param headers 请求头（解析 Range）
     * @return 200 整文件或 206 部分内容
     */
    @GetMapping("/local-files/{*filepath}")
    public ResponseEntity<?> getLocalFile(
            @PathVariable("filepath") String filepath, @RequestHeader HttpHeaders headers) throws IOException {
        Path root = resolveUploadRoot().toAbsolutePath().normalize();
        Path file = root.resolve(filepath).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file) || !Files.isReadable(file)) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(file);
        long contentLength = resource.contentLength();
        MediaType mediaType =
                MediaTypeFactory.getMediaType(filepath).orElse(MediaType.APPLICATION_OCTET_STREAM);

        List<HttpRange> ranges = headers.getRange();
        if (ranges == null || ranges.isEmpty()) {
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(contentLength)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resource);
        }

        HttpRange range = ranges.getFirst();
        long start;
        long end;
        try {
            start = range.getRangeStart(contentLength);
            end = range.getRangeEnd(contentLength);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                    .build();
        }
        long rangeLength = end - start + 1;
        if (rangeLength <= 0 || start >= contentLength) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                    .build();
        }

        ResourceRegion region = new ResourceRegion(resource, start, rangeLength);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(mediaType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + contentLength)
                .contentLength(rangeLength)
                .body(region);
    }

    private Path resolveUploadRoot() {
        String uploadDir = localStorageProperties.uploadDir();
        if (uploadDir == null || uploadDir.isBlank()) {
            return Paths.get("uploads").toAbsolutePath();
        }
        return Paths.get(uploadDir).toAbsolutePath();
    }
}
