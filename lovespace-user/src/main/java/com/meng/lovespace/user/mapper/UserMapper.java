package com.meng.lovespace.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meng.lovespace.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 {@code users} 的 MyBatis-Plus Mapper。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {}

