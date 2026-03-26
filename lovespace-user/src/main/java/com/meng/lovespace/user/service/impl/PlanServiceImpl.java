package com.meng.lovespace.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meng.lovespace.user.dto.PlanCreateRequest;
import com.meng.lovespace.user.dto.PlanResponse;
import com.meng.lovespace.user.dto.PlanTaskCreateRequest;
import com.meng.lovespace.user.dto.PlanTaskReplaceRequest;
import com.meng.lovespace.user.dto.PlanTaskResponse;
import com.meng.lovespace.user.dto.PlanUpdateRequest;
import com.meng.lovespace.user.entity.CoupleBinding;
import com.meng.lovespace.user.entity.Plan;
import com.meng.lovespace.user.entity.PlanTask;
import com.meng.lovespace.user.exception.PlanBusinessException;
import com.meng.lovespace.user.mapper.PlanMapper;
import com.meng.lovespace.user.mapper.PlanTaskMapper;
import com.meng.lovespace.user.service.CoupleBindingService;
import com.meng.lovespace.user.service.PlanService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * {@link PlanService} 实现。
 */
@Slf4j
@Service
public class PlanServiceImpl extends ServiceImpl<PlanMapper, Plan> implements PlanService {

    private static final Set<String> PLAN_TYPES = Set.of("goal", "travel", "event");

    private final CoupleBindingService coupleBindingService;
    private final PlanTaskMapper planTaskMapper;

    public PlanServiceImpl(CoupleBindingService coupleBindingService, PlanTaskMapper planTaskMapper) {
        this.coupleBindingService = coupleBindingService;
        this.planTaskMapper = planTaskMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanResponse createPlan(String userId, PlanCreateRequest req) {
        requireBinding(userId, req.coupleId());
        validatePlanType(req.planType());
        LocalDate start = req.startDate();
        LocalDate end = req.endDate();
        validateDateRange(start, end);
        int priority = req.priority() == null ? 0 : req.priority();
        int progress = req.progress() == null ? 0 : req.progress();
        validateProgress(progress);
        String status = StringUtils.hasText(req.status()) ? req.status().trim() : "draft";

        Plan plan = new Plan();
        plan.setCoupleId(req.coupleId());
        plan.setTitle(req.title().trim());
        plan.setDescription(req.description());
        plan.setPlanType(req.planType().trim().toLowerCase());
        plan.setPriority(priority);
        plan.setStartDate(start);
        plan.setEndDate(end);
        plan.setStatus(status);
        plan.setProgress(progress);
        plan.setBudgetTotal(req.budgetTotal());
        plan.setBudgetSpent(req.budgetSpent());
        save(plan);
        log.info("plan.created id={} coupleId={} userId={}", plan.getId(), plan.getCoupleId(), userId);
        return toPlanResponse(plan, List.of());
    }

    @Override
    public List<PlanResponse> listPlans(String userId, String coupleId) {
        requireBinding(userId, coupleId);
        List<Plan> plans = lambdaQuery().eq(Plan::getCoupleId, coupleId).orderByDesc(Plan::getCreatedAt).list();
        if (plans.isEmpty()) {
            return List.of();
        }
        List<String> planIds = plans.stream().map(Plan::getId).toList();
        LambdaQueryWrapper<PlanTask> q = new LambdaQueryWrapper<>();
        q.in(PlanTask::getPlanId, planIds);
        List<PlanTask> tasks = planTaskMapper.selectList(q);
        Map<String, List<PlanTask>> byPlan =
                tasks.stream().collect(Collectors.groupingBy(PlanTask::getPlanId));
        return plans.stream()
                .map(p -> {
                    // getOrDefault(..., List.of()) 为不可变列表，直接 sort 会抛异常；复制到 ArrayList 再排序
                    List<PlanTask> ts = new ArrayList<>(byPlan.getOrDefault(p.getId(), List.of()));
                    ts.sort(Comparator.comparing(PlanTask::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
                    return toPlanResponse(p, ts.stream().map(PlanServiceImpl::toTaskResponse).toList());
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanResponse updatePlan(String userId, String planId, PlanUpdateRequest req) {
        Plan plan = getById(planId);
        if (plan == null) {
            throw new PlanBusinessException(40480, "plan not found");
        }
        requireBinding(userId, plan.getCoupleId());

        if (StringUtils.hasText(req.title())) {
            plan.setTitle(req.title().trim());
        }
        if (req.description() != null) {
            plan.setDescription(req.description());
        }
        if (StringUtils.hasText(req.planType())) {
            validatePlanType(req.planType());
            plan.setPlanType(req.planType().trim().toLowerCase());
        }
        if (req.priority() != null) {
            plan.setPriority(req.priority());
        }
        if (req.startDate() != null) {
            plan.setStartDate(req.startDate());
        }
        if (req.endDate() != null) {
            plan.setEndDate(req.endDate());
        }
        if (StringUtils.hasText(req.status())) {
            plan.setStatus(req.status().trim());
        }
        if (req.progress() != null) {
            validateProgress(req.progress());
            plan.setProgress(req.progress());
        }
        if (req.budgetTotal() != null) {
            plan.setBudgetTotal(req.budgetTotal());
        }
        if (req.budgetSpent() != null) {
            plan.setBudgetSpent(req.budgetSpent());
        }

        validateDateRange(plan.getStartDate(), plan.getEndDate());
        updateById(plan);
        log.info("plan.updated id={} userId={}", planId, userId);
        List<PlanTaskResponse> taskViews = listTaskResponsesForPlan(planId);
        return toPlanResponse(plan, taskViews);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(String userId, String planId) {
        Plan plan = getById(planId);
        if (plan == null) {
            throw new PlanBusinessException(40480, "plan not found");
        }
        requireBinding(userId, plan.getCoupleId());
        removeById(planId);
        log.info("plan.deleted id={} userId={}", planId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanTaskResponse createTask(String userId, String planId, PlanTaskCreateRequest req) {
        Plan plan = getById(planId);
        if (plan == null) {
            throw new PlanBusinessException(40480, "plan not found");
        }
        CoupleBinding binding = requireBinding(userId, plan.getCoupleId());
        assertAssigneeAllowed(binding, req.assigneeId());

        PlanTask task = new PlanTask();
        task.setPlanId(planId);
        task.setTitle(req.title().trim());
        task.setAssigneeId(StringUtils.hasText(req.assigneeId()) ? req.assigneeId().trim() : null);
        task.setDueDate(req.dueDate());
        task.setIsCompleted(0);
        planTaskMapper.insert(task);
        log.info("plan.task.created planId={} taskId={} userId={}", planId, task.getId(), userId);
        return toTaskResponse(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanTaskResponse updateTask(String userId, String planId, String taskId, PlanTaskReplaceRequest req) {
        Plan plan = getById(planId);
        if (plan == null) {
            throw new PlanBusinessException(40480, "plan not found");
        }
        CoupleBinding binding = requireBinding(userId, plan.getCoupleId());

        PlanTask task = planTaskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getPlanId(), planId)) {
            throw new PlanBusinessException(40481, "task not found");
        }

        assertAssigneeAllowed(binding, req.assigneeId());

        task.setTitle(req.title().trim());
        task.setAssigneeId(StringUtils.hasText(req.assigneeId()) ? req.assigneeId().trim() : null);
        task.setDueDate(req.dueDate());

        if (req.completed()) {
            task.setIsCompleted(1);
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
        } else {
            task.setIsCompleted(0);
            task.setCompletedAt(null);
        }
        planTaskMapper.updateById(task);
        log.info("plan.task.updated planId={} taskId={} userId={}", planId, taskId, userId);
        return toTaskResponse(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(String userId, String planId, String taskId) {
        Plan plan = getById(planId);
        if (plan == null) {
            throw new PlanBusinessException(40480, "plan not found");
        }
        requireBinding(userId, plan.getCoupleId());

        PlanTask task = planTaskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getPlanId(), planId)) {
            throw new PlanBusinessException(40481, "task not found");
        }
        planTaskMapper.deleteById(taskId);
        log.info("plan.task.deleted planId={} taskId={} userId={}", planId, taskId, userId);
    }

    private List<PlanTaskResponse> listTaskResponsesForPlan(String planId) {
        LambdaQueryWrapper<PlanTask> q = new LambdaQueryWrapper<>();
        q.eq(PlanTask::getPlanId, planId).orderByAsc(PlanTask::getCreatedAt);
        return planTaskMapper.selectList(q).stream().map(PlanServiceImpl::toTaskResponse).toList();
    }

    private CoupleBinding requireBinding(String userId, String coupleId) {
        return coupleBindingService
                .findActiveOrFrozenMembership(userId, coupleId)
                .orElseThrow(() -> new PlanBusinessException(40380, "forbidden or invalid couple"));
    }

    private void assertAssigneeAllowed(CoupleBinding binding, String assigneeId) {
        if (!StringUtils.hasText(assigneeId)) {
            return;
        }
        String aid = assigneeId.trim();
        if (!aid.equals(binding.getUserId1()) && !aid.equals(binding.getUserId2())) {
            throw new PlanBusinessException(40081, "assignee must be a couple member");
        }
    }

    private static void validatePlanType(String planType) {
        if (!StringUtils.hasText(planType)) {
            throw new PlanBusinessException(40080, "planType is required");
        }
        String t = planType.trim().toLowerCase();
        if (!PLAN_TYPES.contains(t)) {
            throw new PlanBusinessException(40082, "planType must be goal, travel, or event");
        }
    }

    private static void validateProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new PlanBusinessException(40083, "progress must be between 0 and 100");
        }
    }

    private static void validateDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new PlanBusinessException(40084, "startDate cannot be after endDate");
        }
    }

    private static PlanResponse toPlanResponse(Plan p, List<PlanTaskResponse> tasks) {
        return new PlanResponse(
                p.getId(),
                p.getCoupleId(),
                p.getTitle(),
                p.getDescription(),
                p.getPlanType(),
                p.getPriority(),
                p.getStartDate(),
                p.getEndDate(),
                p.getStatus(),
                p.getProgress(),
                p.getBudgetTotal(),
                p.getBudgetSpent(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                tasks);
    }

    private static PlanTaskResponse toTaskResponse(PlanTask t) {
        boolean done = t.getIsCompleted() != null && t.getIsCompleted() == 1;
        return new PlanTaskResponse(
                t.getId(),
                t.getPlanId(),
                t.getTitle(),
                t.getAssigneeId(),
                done,
                t.getCompletedAt(),
                t.getDueDate(),
                t.getCreatedAt());
    }
}
