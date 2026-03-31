package com.meng.lovespace.user.service;

import com.meng.lovespace.user.dto.TimelineUploadInitRequest;
import com.meng.lovespace.user.dto.TimelineUploadInitResponse;
import com.meng.lovespace.user.dto.TimelineUploadStatusResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * 恋爱时间轴大文件分片上传与合并（断点续传：按状态跳过已上传分片）。
 */
public interface TimelineChunkUploadService {

    /** 创建会话并落盘元数据。 */
    TimelineUploadInitResponse initUpload(String userId, TimelineUploadInitRequest request);

    /** 写入一片；{@code contentLength} 须与该片理论长度一致。 */
    void writeChunk(String userId, String uploadId, int chunkIndex, long contentLength, InputStream body)
            throws IOException;

    /** 查询已上传分片下标。 */
    TimelineUploadStatusResponse status(String userId, String uploadId);

    /** 合并分片并发布到时间轴存储，返回访问 URL。 */
    String complete(String userId, String uploadId);

    /** 放弃会话并删除临时文件。 */
    void abort(String userId, String uploadId);

    /** 删除创建时间超过配置的未完成分片会话（定时任务调用）。 */
    void cleanupExpiredSessions();
}
