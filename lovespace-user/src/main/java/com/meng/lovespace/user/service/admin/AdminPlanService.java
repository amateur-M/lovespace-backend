package com.meng.lovespace.user.service.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meng.lovespace.user.entity.Plan;
import com.meng.lovespace.user.entity.PlanExpense;
import com.meng.lovespace.user.entity.PlanTask;

/** 管理端共同计划服务。 */
public interface AdminPlanService {

    IPage<Plan> pagePlans(String coupleId, long page, long pageSize);

    void deletePlan(String adminUserId, String planId);

    IPage<PlanTask> pageTasks(String planId, long page, long pageSize);

    IPage<PlanExpense> pageExpenses(String planId, long page, long pageSize);
}
