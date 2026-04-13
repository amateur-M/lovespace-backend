package com.meng.lovespace.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meng.lovespace.user.entity.MemorialDay;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 纪念日表 {@code memorial_days} 的 MyBatis-Plus Mapper。
 */
@Mapper
public interface MemorialDayMapper extends BaseMapper<MemorialDay> {

    @Select("SELECT DISTINCT couple_id FROM memorial_days")
    List<String> selectDistinctCoupleIds();
}
