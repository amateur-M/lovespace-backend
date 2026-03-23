package com.meng.lovespace.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meng.lovespace.user.dto.AlbumCreateRequest;
import com.meng.lovespace.user.dto.AlbumResponse;
import com.meng.lovespace.user.dto.PhotoResponse;
import com.meng.lovespace.user.dto.PhotoUploadRequest;
import com.meng.lovespace.user.entity.Album;
import com.meng.lovespace.user.entity.Photo;
import com.meng.lovespace.user.exception.AlbumBusinessException;
import com.meng.lovespace.user.mapper.AlbumMapper;
import com.meng.lovespace.user.mapper.PhotoMapper;
import com.meng.lovespace.user.service.AlbumService;
import com.meng.lovespace.user.service.CoupleBindingService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link AlbumService} 实现：复用情侣成员校验，管理相册与照片。
 */
@Slf4j
@Service
public class AlbumServiceImpl extends ServiceImpl<AlbumMapper, Album> implements AlbumService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final CoupleBindingService coupleBindingService;
    private final PhotoMapper photoMapper;
    private final com.meng.lovespace.user.oss.AvatarStorageService avatarStorageService;

    public AlbumServiceImpl(
            CoupleBindingService coupleBindingService,
            PhotoMapper photoMapper,
            com.meng.lovespace.user.oss.AvatarStorageService avatarStorageService) {
        this.coupleBindingService = coupleBindingService;
        this.photoMapper = photoMapper;
        this.avatarStorageService = avatarStorageService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlbumResponse createAlbum(String userId, AlbumCreateRequest req) {
        assertCoupleMember(userId, req.coupleId());
        Album album = new Album();
        album.setCoupleId(req.coupleId());
        album.setName(req.name().trim());
        album.setCoverImageUrl(req.coverImageUrl());
        save(album);
        log.info("album.created id={} coupleId={} creatorId={}", album.getId(), album.getCoupleId(), userId);
        return toAlbumResponse(album);
    }

    @Override
    public List<AlbumResponse> listAlbums(String userId, String coupleId) {
        assertCoupleMember(userId, coupleId);
        return lambdaQuery()
                .eq(Album::getCoupleId, coupleId)
                .orderByDesc(Album::getCreatedAt)
                .list()
                .stream()
                .map(AlbumServiceImpl::toAlbumResponse)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(String userId, String albumId) {
        Album album = getById(albumId);
        if (album == null) {
            throw new AlbumBusinessException(40461, "album not found");
        }
        assertCoupleMember(userId, album.getCoupleId());

        LambdaQueryWrapper<Photo> photoQuery = new LambdaQueryWrapper<>();
        photoQuery.eq(Photo::getAlbumId, albumId);
        photoMapper.delete(photoQuery);
        removeById(albumId);
        log.info("album.deleted id={} operatorId={}", albumId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PhotoResponse uploadPhoto(String userId, String albumId, MultipartFile file, PhotoUploadRequest req) {
        Album album = getById(albumId);
        if (album == null) {
            throw new AlbumBusinessException(40461, "album not found");
        }
        assertCoupleMember(userId, album.getCoupleId());
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (req != null && req.takenDate() != null && req.takenDate().isAfter(today)) {
            throw new AlbumBusinessException(40063, "takenDate cannot be in the future");
        }

        String imageUrl = avatarStorageService.uploadAlbumPhoto(userId, file);
        Photo photo = new Photo();
        photo.setAlbumId(albumId);
        photo.setUploaderId(userId);
        photo.setImageUrl(imageUrl);
        photo.setThumbnailUrl(req == null ? null : req.thumbnailUrl());
        photo.setDescription(req == null ? null : req.description());
        photo.setLocationJson(req == null ? null : req.locationJson());
        photo.setTakenDate(req == null ? null : req.takenDate());
        photo.setTagsJson(req == null ? null : req.tagsJson());
        photo.setIsFavorite(0);
        photoMapper.insert(photo);

        if (album.getCoverImageUrl() == null || album.getCoverImageUrl().isBlank()) {
            album.setCoverImageUrl(imageUrl);
            updateById(album);
        }
        log.info("album.photo.uploaded albumId={} photoId={} uploaderId={}", albumId, photo.getId(), userId);
        return toPhotoResponse(photo);
    }

    @Override
    public List<PhotoResponse> listPhotos(String userId, String albumId) {
        Album album = getById(albumId);
        if (album == null) {
            throw new AlbumBusinessException(40461, "album not found");
        }
        assertCoupleMember(userId, album.getCoupleId());
        LambdaQueryWrapper<Photo> w = new LambdaQueryWrapper<>();
        w.eq(Photo::getAlbumId, albumId).orderByDesc(Photo::getCreatedAt);
        return photoMapper.selectList(w).stream().map(AlbumServiceImpl::toPhotoResponse).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePhoto(String userId, String albumId, String photoId) {
        Album album = getById(albumId);
        if (album == null) {
            throw new AlbumBusinessException(40461, "album not found");
        }
        assertCoupleMember(userId, album.getCoupleId());
        Photo photo = photoMapper.selectById(photoId);
        if (photo == null || !albumId.equals(photo.getAlbumId())) {
            throw new AlbumBusinessException(40462, "photo not found");
        }
        if (!userId.equals(photo.getUploaderId())) {
            throw new AlbumBusinessException(40362, "only uploader can delete photo");
        }
        photoMapper.deleteById(photoId);
        log.info("album.photo.deleted albumId={} photoId={} operatorId={}", albumId, photoId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setFavorite(String userId, String albumId, String photoId, boolean favorite) {
        Album album = getById(albumId);
        if (album == null) {
            throw new AlbumBusinessException(40461, "album not found");
        }
        assertCoupleMember(userId, album.getCoupleId());
        Photo photo = photoMapper.selectById(photoId);
        if (photo == null || !albumId.equals(photo.getAlbumId())) {
            throw new AlbumBusinessException(40462, "photo not found");
        }
        photo.setIsFavorite(favorite ? 1 : 0);
        photoMapper.updateById(photo);
    }

    private void assertCoupleMember(String userId, String coupleId) {
        coupleBindingService
                .findActiveOrFrozenMembership(userId, coupleId)
                .orElseThrow(() -> new AlbumBusinessException(40361, "forbidden or invalid couple"));
    }

    private static AlbumResponse toAlbumResponse(Album a) {
        return new AlbumResponse(
                a.getId(), a.getCoupleId(), a.getName(), a.getCoverImageUrl(), a.getCreatedAt(), a.getUpdatedAt());
    }

    private static PhotoResponse toPhotoResponse(Photo p) {
        return new PhotoResponse(
                p.getId(),
                p.getAlbumId(),
                p.getUploaderId(),
                p.getImageUrl(),
                p.getThumbnailUrl(),
                p.getDescription(),
                p.getLocationJson(),
                p.getTakenDate(),
                p.getTagsJson(),
                p.getIsFavorite(),
                p.getCreatedAt());
    }
}
