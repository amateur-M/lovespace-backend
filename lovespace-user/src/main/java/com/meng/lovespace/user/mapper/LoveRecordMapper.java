package com.meng.lovespace.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meng.lovespace.user.entity.LoveRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 恋爱记录表 {@code love_records} 的 MyBatis-Plus Mapper。
 */
@Mapper
public interface LoveRecordMapper extends BaseMapper<LoveRecord> {}
