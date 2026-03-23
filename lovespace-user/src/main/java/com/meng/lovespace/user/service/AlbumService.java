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
 * 情侣相册领域服务：校验情侣成员身份后管理相册与照片。
 */
public interface AlbumService extends IService<Album> {

    /** 在当前用户所属情侣下创建相册。 */
    AlbumResponse createAlbum(String userId, AlbumCreateRequest req);

    /** 列出某情侣下全部相册（按创建时间倒序）。 */
    List<AlbumResponse> listAlbums(String userId, String coupleId);

    /** 删除相册及其下所有照片记录。 */
    void deleteAlbum(String userId, String albumId);

    /** 上传照片并落库；无封面时首张图作为封面。 */
    PhotoResponse uploadPhoto(String userId, String albumId, MultipartFile file, PhotoUploadRequest req);

    /** 列出相册内照片（按创建时间倒序）。 */
    List<PhotoResponse> listPhotos(String userId, String albumId);

    /** 仅上传者可删。 */
    void deletePhoto(String userId, String albumId, String photoId);

    /** 情侣任一方可更新收藏标记。 */
    void setFavorite(String userId, String albumId, String photoId, boolean favorite);
}
