package com.meng.lovespace.user.service;

import com.meng.lovespace.user.dto.MediaChunkInitRequest;
import com.meng.lovespace.user.dto.MediaChunkInitResponse;
import com.meng.lovespace.user.dto.MediaChunkStatusResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * 时间轴与相册共用的分片上传：合并后写入各自对象前缀。
 */
public interface ChunkedMediaUploadService {

    MediaChunkInitResponse initUpload(String userId, MediaChunkInitRequest request);

    void writeChunk(String userId, String uploadId, int chunkIndex, long contentLength, InputStream body)
            throws IOException;

    MediaChunkStatusResponse status(String userId, String uploadId);

    String complete(String userId, String uploadId);

    void abort(String userId, String uploadId);

    void cleanupExpiredSessions();
}
