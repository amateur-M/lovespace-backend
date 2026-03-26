package com.meng.lovespace.user.dto;

import java.util.List;

/** 分页查询相册内照片。 */
public record AlbumPhotoPageResponse(long total, long page, long pageSize, List<PhotoResponse> photos) {}
