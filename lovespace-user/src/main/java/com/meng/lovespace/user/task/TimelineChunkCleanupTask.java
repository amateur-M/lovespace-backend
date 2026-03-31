package com.meng.lovespace.user.task;

import com.meng.lovespace.user.service.TimelineChunkUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定期清理超时的分片上传暂存目录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimelineChunkCleanupTask {

    private final TimelineChunkUploadService chunkUploadService;

    /** 每小时扫描一次 {@code _pending/timeline}。 */
    @Scheduled(fixedDelay = 3_600_000)
    public void cleanup() {
        try {
            chunkUploadService.cleanupExpiredSessions();
        } catch (Exception e) {
            log.warn("timeline chunk cleanup failed: {}", e.getMessage());
        }
    }
}
