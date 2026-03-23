package com.meng.lovespace.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meng.lovespace.user.entity.Photo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 照片表 {@code album_photos} 的 MyBatis-Plus Mapper。
 */
@Mapper
public interface PhotoMapper extends BaseMapper<Photo> {}
