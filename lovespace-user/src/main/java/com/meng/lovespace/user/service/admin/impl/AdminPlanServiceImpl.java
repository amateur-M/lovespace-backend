package com.meng.lovespace.user.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.entity.Plan;
import com.meng.lovespace.user.entity.PlanExpense;
import com.meng.lovespace.user.entity.PlanTask;
import com.meng.lovespace.user.mapper.PlanExpenseMapper;
import com.meng.lovespace.user.mapper.PlanMapper;
import com.meng.lovespace.user.mapper.PlanTaskMapper;
import com.meng.lovespace.user.service.admin.AdminPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AdminPlanServiceImpl implements AdminPlanService {

    private final PlanMapper planMapper;
    private final PlanTaskMapper planTaskMapper;
    private final PlanExpenseMapper planExpenseMapper;

    public AdminPlanServiceImpl(
            PlanMapper planMapper, PlanTaskMapper planTaskMapper, PlanExpenseMapper planExpenseMapper) {
        this.planMapper = planMapper;
        this.planTaskMapper = planTaskMapper;
        this.planExpenseMapper = planExpenseMapper;
    }

    @Override
    public IPage<Plan> pagePlans(String coupleId, long page, long pageSize) {
        LambdaQueryWrapper<Plan> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(coupleId)) {
            qw.eq(Plan::getCoupleId, coupleId.trim());
        }
        qw.orderByDesc(Plan::getUpdatedAt);
        return planMapper.selectPage(Page.of(page, pageSize), qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(String adminUserId, String planId) {
        Plan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new ApiBusinessException(40400, "plan not found");
        }
        planTaskMapper.delete(new LambdaQueryWrapper<PlanTask>().eq(PlanTask::getPlanId, planId));
        planExpenseMapper.delete(
                new LambdaQueryWrapper<PlanExpense>().eq(PlanExpense::getPlanId, planId));
        planMapper.deleteById(planId);
        log.info("admin.plans.delete adminUserId={} planId={}", adminUserId, planId);
    }

    @Override
    public IPage<PlanTask> pageTasks(String planId, long page, long pageSize) {
        LambdaQueryWrapper<PlanTask> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(planId)) {
            qw.eq(PlanTask::getPlanId, planId.trim());
        }
        qw.orderByDesc(PlanTask::getCreatedAt);
        return planTaskMapper.selectPage(Page.of(page, pageSize), qw);
    }

    @Override
    public IPage<PlanExpense> pageExpenses(String planId, long page, long pageSize) {
        LambdaQueryWrapper<PlanExpense> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(planId)) {
            qw.eq(PlanExpense::getPlanId, planId.trim());
        }
        qw.orderByDesc(PlanExpense::getCreatedAt);
        return planExpenseMapper.selectPage(Page.of(page, pageSize), qw);
    }
}
