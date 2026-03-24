package com.meng.lovespace.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meng.lovespace.user.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 私密消息表 {@code private_messages} 的 MyBatis-Plus Mapper。
 */
@Mapper
public interface MessageMapper extends BaseMapper<PrivateMessage> {}
