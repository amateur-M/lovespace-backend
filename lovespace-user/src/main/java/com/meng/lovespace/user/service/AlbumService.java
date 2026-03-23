package com.meng.lovespace.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meng.lovespace.user.dto.AlbumCreateRequest;
import com.meng.lovespace.user.dto.AlbumResponse;
import com.meng.lovespace.user.dto.PhotoResponse;
import com.meng.lovespace.user.dto.PhotoUploadRequest;
import com.meng.lovespace.user.entity.Album;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 情侣相册领域服务：相册与照片管理。
 */
public interface AlbumService extends IService<Album> {

    AlbumResponse createAlbum(String userId, AlbumCreateRequest req);

    List<AlbumResponse> listAlbums(String userId, String coupleId);

    void deleteAlbum(String userId, String albumId);

    PhotoResponse uploadPhoto(String userId, String albumId, MultipartFile file, PhotoUploadRequest req);

    List<PhotoResponse> listPhotos(String userId, String albumId);

    void deletePhoto(String userId, String albumId, String photoId);

    void setFavorite(String userId, String albumId, String photoId, boolean favorite);
}
