package com.meng.lovespace.user.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.user.entity.Album;
import com.meng.lovespace.user.entity.Photo;

/** 管理端相册服务。 */
public interface AdminAlbumService {

    IPage<Album> pageAlbums(String coupleId, long page, long pageSize);

    void deleteAlbum(String adminUserId, String albumId);

    IPage<Photo> pagePhotos(String albumId, long page, long pageSize);

    void deletePhoto(String adminUserId, String photoId);
}
