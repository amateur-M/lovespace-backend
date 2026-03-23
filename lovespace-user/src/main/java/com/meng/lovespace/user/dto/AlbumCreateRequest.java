package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建相册请求。
 */
public record AlbumCreateRequest(
        @NotBlank(message = "coupleId is required") String coupleId,
        @NotBlank(message = "name is required") String name,
        String coverImageUrl) {}
