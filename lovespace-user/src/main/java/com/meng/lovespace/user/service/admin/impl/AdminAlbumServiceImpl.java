package com.meng.lovespace.user.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.entity.Album;
import com.meng.lovespace.user.entity.Photo;
import com.meng.lovespace.user.mapper.AlbumMapper;
import com.meng.lovespace.user.mapper.PhotoMapper;
import com.meng.lovespace.user.service.admin.AdminAlbumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AdminAlbumServiceImpl implements AdminAlbumService {

    private final AlbumMapper albumMapper;
    private final PhotoMapper photoMapper;

    public AdminAlbumServiceImpl(AlbumMapper albumMapper, PhotoMapper photoMapper) {
        this.albumMapper = albumMapper;
        this.photoMapper = photoMapper;
    }

    @Override
    public IPage<Album> pageAlbums(String coupleId, long page, long pageSize) {
        LambdaQueryWrapper<Album> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(coupleId)) {
            qw.eq(Album::getCoupleId, coupleId.trim());
        }
        qw.orderByDesc(Album::getUpdatedAt);
        return albumMapper.selectPage(Page.of(page, pageSize), qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(String adminUserId, String albumId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw new ApiBusinessException(40400, "album not found");
        }
        photoMapper.delete(new LambdaQueryWrapper<Photo>().eq(Photo::getAlbumId, albumId));
        albumMapper.deleteById(albumId);
        log.info("admin.albums.delete adminUserId={} albumId={}", adminUserId, albumId);
    }

    @Override
    public IPage<Photo> pagePhotos(String albumId, long page, long pageSize) {
        LambdaQueryWrapper<Photo> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(albumId)) {
            qw.eq(Photo::getAlbumId, albumId.trim());
        }
        qw.orderByDesc(Photo::getCreatedAt);
        return photoMapper.selectPage(Page.of(page, pageSize), qw);
    }

    @Override
    public void deletePhoto(String adminUserId, String photoId) {
        if (!photoMapper.exists(new LambdaQueryWrapper<Photo>().eq(Photo::getId, photoId))) {
            throw new ApiBusinessException(40400, "photo not found");
        }
        photoMapper.deleteById(photoId);
        log.info("admin.photos.delete adminUserId={} photoId={}", adminUserId, photoId);
    }
}
