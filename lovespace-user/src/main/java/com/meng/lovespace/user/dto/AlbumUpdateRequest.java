package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 更新相册（当前仅名称）。 */
public record AlbumUpdateRequest(
        @NotBlank(message = "name is required") @Size(max = 128, message = "name too long") String name) {}
