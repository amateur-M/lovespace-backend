package com.meng.lovespace.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meng.lovespace.user.entity.PlanExpense;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 计划消费表 {@code plan_expenses} 的 Mapper。
 */
@Mapper
public interface PlanExpenseMapper extends BaseMapper<PlanExpense> {

    @Select("SELECT COALESCE(SUM(amount), 0) FROM plan_expenses WHERE plan_id = #{planId}")
    BigDecimal sumAmountByPlanId(@Param("planId") String planId);
}
