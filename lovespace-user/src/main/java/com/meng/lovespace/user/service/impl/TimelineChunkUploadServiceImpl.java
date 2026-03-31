package com.meng.lovespace.user.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meng.lovespace.user.config.LocalStorageProperties;
import com.meng.lovespace.user.config.TimelineUploadProperties;
import com.meng.lovespace.user.dto.TimelineUploadInitRequest;
import com.meng.lovespace.user.dto.TimelineUploadInitResponse;
import com.meng.lovespace.user.dto.TimelineUploadStatusResponse;
import com.meng.lovespace.user.exception.TimelineBusinessException;
import com.meng.lovespace.user.oss.AvatarStorageService;
import com.meng.lovespace.user.service.TimelineChunkUploadService;
import com.meng.lovespace.user.timeline.TimelineUploadSessionMeta;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 分片目录 {@code <upload-dir>/_pending/timeline/<uploadId>/}，分片文件名为 6 位数字；合并后调用 {@link AvatarStorageService#uploadTimelineFromLocalFile}。
 */
@Slf4j
@Service
public class TimelineChunkUploadServiceImpl implements TimelineChunkUploadService {

    private static final String META_FILE = "meta.json";
    private static final String MERGED_TMP = "_merged.bin";

    private final LocalStorageProperties localStorageProperties;
    private final TimelineUploadProperties timelineUploadProperties;
    private final AvatarStorageService avatarStorageService;
    private final ObjectMapper objectMapper;

    /**
     * @param localStorageProperties 本地上传根（分片暂存于其下 {@code _pending}）
     * @param timelineUploadProperties 分片大小、类型与上限
     * @param avatarStorageService 合并后发布
     * @param objectMapper 读写 {@code meta.json}
     */
    public TimelineChunkUploadServiceImpl(
            LocalStorageProperties localStorageProperties,
            TimelineUploadProperties timelineUploadProperties,
            AvatarStorageService avatarStorageService,
            ObjectMapper objectMapper) {
        this.localStorageProperties = localStorageProperties;
        this.timelineUploadProperties = timelineUploadProperties;
        this.avatarStorageService = avatarStorageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public TimelineUploadInitResponse initUpload(String userId, TimelineUploadInitRequest request) {
        String fileName = request.fileName().trim();
        String ext = extensionOf(fileName);
        List<String> imgExt = safeList(timelineUploadProperties.imageExtensions(), List.of("jpg", "jpeg", "png", "webp"));
        List<String> vidExt = safeList(timelineUploadProperties.videoExtensions(), List.of("mp4", "webm", "mov"));
        List<String> imgLower = imgExt.stream().map(String::toLowerCase).toList();
        List<String> vidLower = vidExt.stream().map(String::toLowerCase).toList();
        boolean isImage = imgLower.contains(ext);
        boolean isVideo = vidLower.contains(ext);
        if (!isImage && !isVideo) {
            throw new TimelineBusinessException(40012, "invalid media type for chunked upload");
        }
        long maxBytes = isVideo ? timelineUploadProperties.videoMaxSizeBytes() : timelineUploadProperties.imageMaxSizeBytes();
        if (request.fileSize() > maxBytes) {
            throw new TimelineBusinessException(
                    40011, (isVideo ? "video" : "image") + " too large, max " + maxBytes / 1024 / 1024 + "MB");
        }

        long chunkSize = effectiveChunkSize();
        int totalChunks = (int) ((request.fileSize() + chunkSize - 1) / chunkSize);
        if (totalChunks <= 0) {
            throw new TimelineBusinessException(40018, "invalid fileSize for chunking");
        }

        UUID uploadId = UUID.randomUUID();
        Path sessionDir = sessionDir(uploadId.toString());
        try {
            Files.createDirectories(sessionDir);
        } catch (IOException e) {
            throw new IllegalStateException("create upload session dir failed", e);
        }

        TimelineUploadSessionMeta meta = new TimelineUploadSessionMeta();
        meta.userId = userId;
        meta.originalFilename = fileName;
        meta.ext = ext;
        meta.totalSize = request.fileSize();
        meta.chunkSize = chunkSize;
        meta.totalChunks = totalChunks;
        meta.video = isVideo;
        meta.contentType = request.contentType();
        meta.createdAtEpochMillis = System.currentTimeMillis();

        try {
            objectMapper.writeValue(sessionDir.resolve(META_FILE).toFile(), meta);
        } catch (IOException e) {
            deleteDirQuietly(sessionDir);
            throw new IllegalStateException("write upload meta failed", e);
        }

        log.info(
                "timeline.chunk.init userId={} uploadId={} totalChunks={} chunkSize={}",
                userId,
                uploadId,
                totalChunks,
                chunkSize);
        return new TimelineUploadInitResponse(uploadId.toString(), chunkSize, totalChunks);
    }

    @Override
    public void writeChunk(String userId, String uploadId, int chunkIndex, long contentLength, InputStream body)
            throws IOException {
        TimelineUploadSessionMeta meta = loadMetaRequireOwner(userId, uploadId);
        if (chunkIndex < 0 || chunkIndex >= meta.totalChunks) {
            throw new TimelineBusinessException(40014, "chunk index out of range");
        }
        if (contentLength < 0) {
            throw new TimelineBusinessException(40022, "Content-Length header required");
        }
        long expected = expectedChunkBytes(meta, chunkIndex);
        if (contentLength != expected) {
            throw new TimelineBusinessException(
                    40015, "Content-Length must be " + expected + " for chunk " + chunkIndex);
        }

        Path sessionDir = sessionDir(uploadId);
        Path chunkFile = sessionDir.resolve(chunkFileName(chunkIndex));
        try (OutputStream os = Files.newOutputStream(chunkFile)) {
            byte[] buf = new byte[65536];
            long remaining = expected;
            while (remaining > 0) {
                int n = body.read(buf, 0, (int) Math.min(buf.length, remaining));
                if (n < 0) {
                    Files.deleteIfExists(chunkFile);
                    throw new TimelineBusinessException(40019, "chunk body shorter than Content-Length");
                }
                os.write(buf, 0, n);
                remaining -= n;
            }
            while (true) {
                int n = body.read(buf);
                if (n < 0) {
                    break;
                }
                if (n > 0) {
                    Files.deleteIfExists(chunkFile);
                    throw new TimelineBusinessException(40021, "chunk body longer than Content-Length");
                }
            }
        }

        log.debug("timeline.chunk.write userId={} uploadId={} index={} bytes={}", userId, uploadId, chunkIndex, expected);
    }

    @Override
    public TimelineUploadStatusResponse status(String userId, String uploadId) {
        TimelineUploadSessionMeta meta = loadMetaRequireOwner(userId, uploadId);
        Path sessionDir = sessionDir(uploadId);
        List<Integer> uploaded = new ArrayList<>();
        for (int i = 0; i < meta.totalChunks; i++) {
            if (Files.isRegularFile(sessionDir.resolve(chunkFileName(i)))) {
                uploaded.add(i);
            }
        }
        uploaded.sort(Comparator.naturalOrder());
        boolean complete = uploaded.size() == meta.totalChunks;
        return new TimelineUploadStatusResponse(
                uploadId, meta.chunkSize, meta.totalChunks, meta.totalSize, uploaded, complete);
    }

    @Override
    public String complete(String userId, String uploadId) {
        TimelineUploadSessionMeta meta = loadMetaRequireOwner(userId, uploadId);
        Path sessionDir = sessionDir(uploadId);
        for (int i = 0; i < meta.totalChunks; i++) {
            if (!Files.isRegularFile(sessionDir.resolve(chunkFileName(i)))) {
                throw new TimelineBusinessException(40016, "missing chunk " + i);
            }
        }

        Path merged = sessionDir.resolve(MERGED_TMP);
        try (OutputStream os = Files.newOutputStream(merged)) {
            for (int i = 0; i < meta.totalChunks; i++) {
                Path part = sessionDir.resolve(chunkFileName(i));
                Files.copy(part, os);
            }
        } catch (IOException e) {
            throw new IllegalStateException("merge chunks failed", e);
        }

        long mergedSize;
        try {
            mergedSize = Files.size(merged);
        } catch (IOException e) {
            throw new IllegalStateException("merged file size failed", e);
        }
        if (mergedSize != meta.totalSize) {
            try {
                Files.deleteIfExists(merged);
            } catch (IOException ignored) {
                // ignore
            }
            throw new TimelineBusinessException(40020, "merged size mismatch");
        }

        String ct =
                meta.contentType == null || meta.contentType.isBlank()
                        ? guessContentType(meta.originalFilename)
                        : meta.contentType;
        String url = avatarStorageService.uploadTimelineFromLocalFile(userId, merged, meta.originalFilename, ct);
        deleteDirQuietly(sessionDir);

        log.info("timeline.chunk.complete userId={} uploadId={} url={}", userId, uploadId, url);
        return url;
    }

    @Override
    public void abort(String userId, String uploadId) {
        parseUploadId(uploadId);
        Path sessionDir = sessionDir(uploadId);
        if (!Files.isDirectory(sessionDir)) {
            throw new TimelineBusinessException(40454, "upload session not found");
        }
        TimelineUploadSessionMeta meta = readSessionMeta(sessionDir);
        if (meta == null) {
            throw new TimelineBusinessException(40454, "upload session not found");
        }
        if (!userId.equals(meta.userId)) {
            throw new TimelineBusinessException(40355, "forbidden for this upload session");
        }
        deleteDirQuietly(sessionDir);
        log.info("timeline.chunk.abort userId={} uploadId={}", userId, uploadId);
    }

    @Override
    public void cleanupExpiredSessions() {
        Path pending = pendingRoot();
        if (!Files.isDirectory(pending)) {
            return;
        }
        long ttlMs = Math.max(1L, effectivePendingHours()) * 3600_000L;
        long now = System.currentTimeMillis();
        try (Stream<Path> stream = Files.list(pending)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                try {
                    TimelineUploadSessionMeta meta = readSessionMeta(dir);
                    if (meta == null) {
                        return;
                    }
                    if (now - meta.createdAtEpochMillis > ttlMs) {
                        deleteDirQuietly(dir);
                        log.info("timeline.chunk.cleanup expired uploadId={}", dir.getFileName());
                    }
                } catch (Exception e) {
                    log.warn("timeline.chunk.cleanup skip dir={} reason={}", dir, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.warn("timeline.chunk.cleanup list failed: {}", e.getMessage());
        }
    }

    private TimelineUploadSessionMeta loadMetaRequireOwner(String userId, String uploadId) {
        parseUploadId(uploadId);
        Path dir = sessionDir(uploadId);
        TimelineUploadSessionMeta meta = readSessionMeta(dir);
        if (meta == null) {
            throw new TimelineBusinessException(40454, "upload session not found");
        }
        if (!userId.equals(meta.userId)) {
            throw new TimelineBusinessException(40355, "forbidden for this upload session");
        }
        return meta;
    }

    private TimelineUploadSessionMeta readSessionMeta(Path sessionDir) {
        Path metaPath = sessionDir.resolve(META_FILE);
        if (!Files.isRegularFile(metaPath)) {
            return null;
        }
        try {
            return objectMapper.readValue(metaPath.toFile(), TimelineUploadSessionMeta.class);
        } catch (IOException e) {
            return null;
        }
    }

    private static void parseUploadId(String uploadId) {
        try {
            UUID.fromString(uploadId);
        } catch (IllegalArgumentException e) {
            throw new TimelineBusinessException(40017, "invalid uploadId");
        }
    }

    private Path sessionDir(String uploadId) {
        return pendingRoot().resolve(uploadId);
    }

    private Path pendingRoot() {
        return resolveUploadRoot().resolve("_pending").resolve("timeline");
    }

    private Path resolveUploadRoot() {
        String uploadDir = localStorageProperties.uploadDir();
        if (uploadDir == null || uploadDir.isBlank()) {
            return Paths.get("uploads").toAbsolutePath();
        }
        return Paths.get(uploadDir).toAbsolutePath();
    }

    private long effectiveChunkSize() {
        long c = timelineUploadProperties.chunkSizeBytes();
        return c > 0 ? c : 5L * 1024 * 1024;
    }

    private long effectivePendingHours() {
        long h = timelineUploadProperties.pendingSessionHours();
        return h > 0 ? h : 48L;
    }

    private static long expectedChunkBytes(TimelineUploadSessionMeta meta, int chunkIndex) {
        if (chunkIndex == meta.totalChunks - 1) {
            long r = meta.totalSize - (long) chunkIndex * meta.chunkSize;
            return Math.max(1L, r);
        }
        return meta.chunkSize;
    }

    private static String chunkFileName(int index) {
        return "%06d".formatted(index);
    }

    private static String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private static List<String> safeList(List<String> list, List<String> fallback) {
        if (list == null || list.isEmpty()) {
            return fallback;
        }
        return list;
    }

    private static String guessContentType(String name) {
        String g = java.net.URLConnection.guessContentTypeFromName(name);
        return g != null ? g : "application/octet-stream";
    }

    private static void deleteDirQuietly(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // ignore
                }
            });
        } catch (IOException ignored) {
            // ignore
        }
    }
}
