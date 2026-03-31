package com.meng.lovespace.user.timeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 分片上传会话元数据（持久化在 {@code meta.json}）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimelineUploadSessionMeta {

    public String userId;
    public String originalFilename;
    public String ext;
    public long totalSize;
    public long chunkSize;
    public int totalChunks;
    public boolean video;
    /** 可选，供 MinIO 等写入 Content-Type */
    public String contentType;
    public long createdAtEpochMillis;
}
