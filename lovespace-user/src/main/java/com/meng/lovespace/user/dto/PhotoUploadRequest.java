package com.meng.lovespace.user.dto;

import java.time.LocalDate;

/**
 * 上传相册照片时的附加元数据。
 */
public record PhotoUploadRequest(
        String thumbnailUrl,
        String description,
        String locationJson,
        LocalDate takenDate,
        String tagsJson) {}
