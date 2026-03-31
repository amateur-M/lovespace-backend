package com.meng.lovespace.user.task;

import com.meng.lovespace.user.service.ChunkedMediaUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaChunkCleanupTask {

    private final ChunkedMediaUploadService chunkedMediaUploadService;

    @Scheduled(fixedDelay = 3_600_000)
    public void cleanup() {
        try {
            chunkedMediaUploadService.cleanupExpiredSessions();
        } catch (Exception e) {
            log.warn("media chunk cleanup failed: {}", e.getMessage());
        }
    }
}
