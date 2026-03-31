package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * 将已通过分片（或直传）写入对象存储的相册图 URL 登记为照片记录。
 */
public record PhotoRegisterFromUrlRequest(
        @NotBlank(message = "imageUrl is required") String imageUrl,
        String thumbnailUrl,
        String description,
        String locationJson,
        LocalDate takenDate,
        String tagsJson) {}
