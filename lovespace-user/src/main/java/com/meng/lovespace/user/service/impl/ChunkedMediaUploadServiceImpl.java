package com.meng.lovespace.user.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meng.lovespace.user.config.AvatarUploadProperties;
import com.meng.lovespace.user.config.LocalStorageProperties;
import com.meng.lovespace.user.config.TimelineUploadProperties;
import com.meng.lovespace.user.dto.MediaChunkInitRequest;
import com.meng.lovespace.user.dto.MediaChunkInitResponse;
import com.meng.lovespace.user.dto.MediaChunkStatusResponse;
import com.meng.lovespace.user.entity.Album;
import com.meng.lovespace.user.exception.MediaChunkBusinessException;
import com.meng.lovespace.user.mapper.AlbumMapper;
import com.meng.lovespace.user.oss.AvatarStorageService;
import com.meng.lovespace.user.service.ChunkedMediaUploadService;
import com.meng.lovespace.user.service.CoupleBindingService;
import com.meng.lovespace.user.upload.MediaChunkTarget;
import com.meng.lovespace.user.upload.MediaUploadSessionMeta;
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
 * 分片暂存 {@code _pending/media/<uploadId>/}；兼容清理旧路径 {@code _pending/timeline/}。
 */
@Slf4j
@Service
public class ChunkedMediaUploadServiceImpl implements ChunkedMediaUploadService {

    private static final String META_FILE = "meta.json";
    private static final String MERGED_TMP = "_merged.bin";

    private final LocalStorageProperties localStorageProperties;
    private final TimelineUploadProperties timelineUploadProperties;
    private final AvatarUploadProperties avatarUploadProperties;
    private final AvatarStorageService avatarStorageService;
    private final ObjectMapper objectMapper;
    private final AlbumMapper albumMapper;
    private final CoupleBindingService coupleBindingService;

    public ChunkedMediaUploadServiceImpl(
            LocalStorageProperties localStorageProperties,
            TimelineUploadProperties timelineUploadProperties,
            AvatarUploadProperties avatarUploadProperties,
            AvatarStorageService avatarStorageService,
            ObjectMapper objectMapper,
            AlbumMapper albumMapper,
            CoupleBindingService coupleBindingService) {
        this.localStorageProperties = localStorageProperties;
        this.timelineUploadProperties = timelineUploadProperties;
        this.avatarUploadProperties = avatarUploadProperties;
        this.avatarStorageService = avatarStorageService;
        this.objectMapper = objectMapper;
        this.albumMapper = albumMapper;
        this.coupleBindingService = coupleBindingService;
    }

    @Override
    public MediaChunkInitResponse initUpload(String userId, MediaChunkInitRequest request) {
        final MediaChunkTarget target;
        try {
            target = MediaChunkTarget.fromString(request.target());
        } catch (Exception e) {
            throw new MediaChunkBusinessException(40023, "invalid target, use TIMELINE or ALBUM");
        }

        String albumId = request.albumId() == null ? null : request.albumId().trim();
        if (target == MediaChunkTarget.ALBUM) {
            if (albumId == null || albumId.isBlank()) {
                throw new MediaChunkBusinessException(40024, "albumId is required for ALBUM target");
            }
            assertAlbumWritable(userId, albumId);
        }

        String fileName = request.fileName().trim();
        String ext = extensionOf(fileName);

        boolean isVideo;
        long maxBytes;
        if (target == MediaChunkTarget.ALBUM) {
            List<String> allowed = albumAllowedExtensions();
            if (!allowed.contains(ext)) {
                throw new MediaChunkBusinessException(
                        40012, "invalid image type for album, allowed: " + String.join(",", allowed));
            }
            isVideo = false;
            maxBytes = timelineUploadProperties.imageMaxSizeBytes();
        } else {
            List<String> imgExt = safeList(timelineUploadProperties.imageExtensions(), List.of("jpg", "jpeg", "png", "webp"));
            List<String> vidExt = safeList(timelineUploadProperties.videoExtensions(), List.of("mp4", "webm", "mov"));
            List<String> imgLower = imgExt.stream().map(String::toLowerCase).toList();
            List<String> vidLower = vidExt.stream().map(String::toLowerCase).toList();
            boolean isImage = imgLower.contains(ext);
            isVideo = vidLower.contains(ext);
            if (!isImage && !isVideo) {
                throw new MediaChunkBusinessException(40012, "invalid media type for chunked upload");
            }
            maxBytes = isVideo ? timelineUploadProperties.videoMaxSizeBytes() : timelineUploadProperties.imageMaxSizeBytes();
        }

        if (request.fileSize() > maxBytes) {
            String kind = target == MediaChunkTarget.ALBUM ? "image" : (isVideo ? "video" : "image");
            throw new MediaChunkBusinessException(
                    40011, kind + " too large, max " + maxBytes / 1024 / 1024 + "MB");
        }

        long chunkSize = effectiveChunkSize();
        int totalChunks = (int) ((request.fileSize() + chunkSize - 1) / chunkSize);
        if (totalChunks <= 0) {
            throw new MediaChunkBusinessException(40018, "invalid fileSize for chunking");
        }

        UUID uploadId = UUID.randomUUID();
        Path sessionDir = pendingMediaRoot().resolve(uploadId.toString());
        try {
            Files.createDirectories(sessionDir);
        } catch (IOException e) {
            throw new IllegalStateException("create upload session dir failed", e);
        }

        MediaUploadSessionMeta meta = new MediaUploadSessionMeta();
        meta.userId = userId;
        meta.target = target.name();
        meta.albumId = target == MediaChunkTarget.ALBUM ? albumId : null;
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
                "media.chunk.init userId={} target={} uploadId={} totalChunks={} chunkSize={}",
                userId,
                target,
                uploadId,
                totalChunks,
                chunkSize);
        return new MediaChunkInitResponse(uploadId.toString(), chunkSize, totalChunks);
    }

    @Override
    public void writeChunk(String userId, String uploadId, int chunkIndex, long contentLength, InputStream body)
            throws IOException {
        MediaUploadSessionMeta meta = loadMetaRequireOwner(userId, uploadId);
        if (chunkIndex < 0 || chunkIndex >= meta.totalChunks) {
            throw new MediaChunkBusinessException(40014, "chunk index out of range");
        }
        if (contentLength < 0) {
            throw new MediaChunkBusinessException(40022, "Content-Length header required");
        }
        long expected = expectedChunkBytes(meta, chunkIndex);
        if (contentLength != expected) {
            throw new MediaChunkBusinessException(
                    40015, "Content-Length must be " + expected + " for chunk " + chunkIndex);
        }

        Path sessionDir = requireSessionDir(uploadId);
        Path chunkFile = sessionDir.resolve(chunkFileName(chunkIndex));
        try (OutputStream os = Files.newOutputStream(chunkFile)) {
            byte[] buf = new byte[65536];
            long remaining = expected;
            while (remaining > 0) {
                int n = body.read(buf, 0, (int) Math.min(buf.length, remaining));
                if (n < 0) {
                    Files.deleteIfExists(chunkFile);
                    throw new MediaChunkBusinessException(40019, "chunk body shorter than Content-Length");
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
                    throw new MediaChunkBusinessException(40021, "chunk body longer than Content-Length");
                }
            }
        }

        log.debug("media.chunk.write userId={} uploadId={} index={} bytes={}", userId, uploadId, chunkIndex, expected);
    }

    @Override
    public MediaChunkStatusResponse status(String userId, String uploadId) {
        MediaUploadSessionMeta meta = loadMetaRequireOwner(userId, uploadId);
        Path sessionDir = requireSessionDir(uploadId);
        List<Integer> uploaded = new ArrayList<>();
        for (int i = 0; i < meta.totalChunks; i++) {
            if (Files.isRegularFile(sessionDir.resolve(chunkFileName(i)))) {
                uploaded.add(i);
            }
        }
        uploaded.sort(Comparator.naturalOrder());
        boolean complete = uploaded.size() == meta.totalChunks;
        return new MediaChunkStatusResponse(
                uploadId, meta.chunkSize, meta.totalChunks, meta.totalSize, uploaded, complete);
    }

    @Override
    public String complete(String userId, String uploadId) {
        MediaUploadSessionMeta meta = loadMetaRequireOwner(userId, uploadId);
        Path sessionDir = requireSessionDir(uploadId);
        for (int i = 0; i < meta.totalChunks; i++) {
            if (!Files.isRegularFile(sessionDir.resolve(chunkFileName(i)))) {
                throw new MediaChunkBusinessException(40016, "missing chunk " + i);
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
            throw new MediaChunkBusinessException(40020, "merged size mismatch");
        }

        String ct =
                meta.contentType == null || meta.contentType.isBlank()
                        ? guessContentType(meta.originalFilename)
                        : meta.contentType;

        MediaChunkTarget target =
                meta.target == null || meta.target.isBlank()
                        ? MediaChunkTarget.TIMELINE
                        : MediaChunkTarget.valueOf(meta.target);
        String url;
        if (target == MediaChunkTarget.TIMELINE) {
            url = avatarStorageService.uploadTimelineFromLocalFile(userId, merged, meta.originalFilename, ct);
        } else {
            url = avatarStorageService.uploadAlbumFromLocalFile(userId, merged, meta.originalFilename, ct);
        }
        deleteDirQuietly(sessionDir);

        log.info("media.chunk.complete userId={} target={} uploadId={} url={}", userId, target, uploadId, url);
        return url;
    }

    @Override
    public void abort(String userId, String uploadId) {
        parseUploadId(uploadId);
        Path dir = resolveExistingSessionDir(uploadId);
        if (dir == null || !Files.isDirectory(dir)) {
            throw new MediaChunkBusinessException(40454, "upload session not found");
        }
        MediaUploadSessionMeta meta = readSessionMeta(dir);
        if (meta == null) {
            throw new MediaChunkBusinessException(40454, "upload session not found");
        }
        if (!userId.equals(meta.userId)) {
            throw new MediaChunkBusinessException(40355, "forbidden for this upload session");
        }
        deleteDirQuietly(dir);
        log.info("media.chunk.abort userId={} uploadId={}", userId, uploadId);
    }

    @Override
    public void cleanupExpiredSessions() {
        long ttlMs = Math.max(1L, effectivePendingHours()) * 3600_000L;
        long now = System.currentTimeMillis();
        cleanupPendingSubdir(pendingMediaRoot(), ttlMs, now);
        cleanupPendingSubdir(legacyTimelinePendingRoot(), ttlMs, now);
    }

    private void cleanupPendingSubdir(Path pending, long ttlMs, long now) {
        if (!Files.isDirectory(pending)) {
            return;
        }
        try (Stream<Path> stream = Files.list(pending)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                try {
                    MediaUploadSessionMeta meta = readSessionMeta(dir);
                    if (meta == null) {
                        return;
                    }
                    if (now - meta.createdAtEpochMillis > ttlMs) {
                        deleteDirQuietly(dir);
                        log.info("media.chunk.cleanup expired uploadId={}", dir.getFileName());
                    }
                } catch (Exception e) {
                    log.warn("media.chunk.cleanup skip dir={} reason={}", dir, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.warn("media.chunk.cleanup list failed: {}", e.getMessage());
        }
    }

    private void assertAlbumWritable(String userId, String albumId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw new MediaChunkBusinessException(40461, "album not found");
        }
        coupleBindingService
                .findActiveOrFrozenMembership(userId, album.getCoupleId())
                .orElseThrow(() -> new MediaChunkBusinessException(40361, "forbidden or invalid couple"));
    }

    private List<String> albumAllowedExtensions() {
        List<String> allowed = avatarUploadProperties.allowedExtensions();
        if (allowed == null || allowed.isEmpty()) {
            allowed = List.of("jpg", "jpeg", "png", "webp");
        }
        return allowed.stream().map(String::toLowerCase).toList();
    }

    private MediaUploadSessionMeta loadMetaRequireOwner(String userId, String uploadId) {
        parseUploadId(uploadId);
        Path dir = resolveExistingSessionDir(uploadId);
        if (dir == null) {
            throw new MediaChunkBusinessException(40454, "upload session not found");
        }
        MediaUploadSessionMeta meta = readSessionMeta(dir);
        if (meta == null) {
            throw new MediaChunkBusinessException(40454, "upload session not found");
        }
        if (!userId.equals(meta.userId)) {
            throw new MediaChunkBusinessException(40355, "forbidden for this upload session");
        }
        return meta;
    }

    private MediaUploadSessionMeta readSessionMeta(Path sessionDir) {
        Path metaPath = sessionDir.resolve(META_FILE);
        if (!Files.isRegularFile(metaPath)) {
            return null;
        }
        try {
            return objectMapper.readValue(metaPath.toFile(), MediaUploadSessionMeta.class);
        } catch (IOException e) {
            return null;
        }
    }

    private static void parseUploadId(String uploadId) {
        try {
            UUID.fromString(uploadId);
        } catch (IllegalArgumentException e) {
            throw new MediaChunkBusinessException(40017, "invalid uploadId");
        }
    }

    private Path requireSessionDir(String uploadId) {
        Path dir = resolveExistingSessionDir(uploadId);
        if (dir == null) {
            throw new MediaChunkBusinessException(40454, "upload session not found");
        }
        return dir;
    }

    /** 新会话仅在 {@code media} 下创建；兼容旧版 {@code timeline} 目录。 */
    private Path resolveExistingSessionDir(String uploadId) {
        Path m = pendingMediaRoot().resolve(uploadId);
        if (Files.isDirectory(m)) {
            return m;
        }
        Path t = legacyTimelinePendingRoot().resolve(uploadId);
        if (Files.isDirectory(t)) {
            return t;
        }
        return null;
    }

    private Path pendingMediaRoot() {
        return resolveUploadRoot().resolve("_pending").resolve("media");
    }

    private Path legacyTimelinePendingRoot() {
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

    private static long expectedChunkBytes(MediaUploadSessionMeta meta, int chunkIndex) {
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
