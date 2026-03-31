package com.meng.lovespace.user.upload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 分片会话元数据（{@code meta.json}）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaUploadSessionMeta {

    public String userId;
    /** {@link MediaChunkTarget#name()} */
    public String target;
    /** {@link MediaChunkTarget#ALBUM} 时必填 */
    public String albumId;
    public String originalFilename;
    public String ext;
    public long totalSize;
    public long chunkSize;
    public int totalChunks;
    /** 时间轴：是否视频；相册：恒为 false */
    public boolean video;
    public String contentType;
    public long createdAtEpochMillis;
}
