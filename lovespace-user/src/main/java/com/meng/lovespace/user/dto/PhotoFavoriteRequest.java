package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 照片收藏状态更新请求。
 */
public record PhotoFavoriteRequest(@NotNull(message = "isFavorite is required") Boolean isFavorite) {}
