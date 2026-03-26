package com.meng.lovespace.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meng.lovespace.user.dto.PlanCreateRequest;
import com.meng.lovespace.user.dto.PlanResponse;
import com.meng.lovespace.user.dto.PlanTaskCreateRequest;
import com.meng.lovespace.user.dto.PlanTaskResponse;
import com.meng.lovespace.user.dto.PlanUpdateRequest;
import com.meng.lovespace.user.entity.Plan;
import java.util.List;

/**
 * 共同计划：校验情侣成员身份后管理计划与子任务。
 */
public interface PlanService extends IService<Plan> {

    /** 创建计划。 */
    PlanResponse createPlan(String userId, PlanCreateRequest req);

    /** 列出某情侣下全部计划（按创建时间倒序），含子任务。 */
    List<PlanResponse> listPlans(String userId, String coupleId);

    /** 更新计划。 */
    PlanResponse updatePlan(String userId, String planId, PlanUpdateRequest req);

    /** 删除计划（级联删除子任务由数据库或业务保证）。 */
    void deletePlan(String userId, String planId);

    /** 在计划下创建子任务。 */
    PlanTaskResponse createTask(String userId, String planId, PlanTaskCreateRequest req);

    /** 将子任务标记为已完成。 */
    PlanTaskResponse completeTask(String userId, String planId, String taskId);
}
