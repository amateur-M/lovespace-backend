package com.meng.lovespace.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meng.lovespace.user.couple.CoupleBindingStatus;
import com.meng.lovespace.user.couple.RelationshipDaysCalculator;
import com.meng.lovespace.user.dto.CoupleAcceptRequest;
import com.meng.lovespace.user.dto.CoupleInfoResponse;
import com.meng.lovespace.user.dto.CoupleInviteResponse;
import com.meng.lovespace.user.dto.CouplePendingInviteResponse;
import com.meng.lovespace.user.dto.UserResponse;
import com.meng.lovespace.user.entity.CoupleBinding;
import com.meng.lovespace.user.entity.User;
import com.meng.lovespace.user.exception.CoupleBindingBusinessException;
import com.meng.lovespace.user.mapper.CoupleBindingMapper;
import com.meng.lovespace.user.service.CoupleBindingService;
import com.meng.lovespace.user.service.UserService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CoupleBindingService} 实现：校验用户状态、邀请方向、恋爱天数计算与持久化。
 */
@Service
public class CoupleBindingServiceImpl extends ServiceImpl<CoupleBindingMapper, CoupleBinding>
        implements CoupleBindingService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final UserService userService;

    public CoupleBindingServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoupleInviteResponse invite(String inviterId, String inviteeUserId) {
        if (inviterId.equals(inviteeUserId)) {
            throw new CoupleBindingBusinessException(40002, "cannot invite yourself");
        }
        User invitee = userService.getById(inviteeUserId);
        if (invitee == null) {
            throw new CoupleBindingBusinessException(40401, "invitee not found");
        }
        if (userHasNonSeparatedBinding(inviterId)) {
            throw new CoupleBindingBusinessException(40901, "you already have a pending or active couple binding");
        }
        if (userHasNonSeparatedBinding(inviteeUserId)) {
            throw new CoupleBindingBusinessException(40902, "target user already has a pending or active couple binding");
        }
        if (pendingBetween(inviterId, inviteeUserId)) {
            throw new CoupleBindingBusinessException(40903, "there is already a pending invite between you two");
        }

        CoupleBinding row = new CoupleBinding();
        row.setUserId1(inviterId);
        row.setUserId2(inviteeUserId);
        row.setStatus(CoupleBindingStatus.PENDING);
        row.setStartDate(null);
        row.setRelationshipDays(0);
        save(row);
        return new CoupleInviteResponse(row.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoupleInfoResponse accept(String currentUserId, CoupleAcceptRequest request) {
        CoupleBinding binding = getById(request.bindingId());
        if (binding == null) {
            throw new CoupleBindingBusinessException(40403, "binding not found");
        }
        if (binding.getStatus() == null || binding.getStatus() != CoupleBindingStatus.PENDING) {
            throw new CoupleBindingBusinessException(40005, "binding is not pending");
        }
        if (!currentUserId.equals(binding.getUserId2())) {
            throw new CoupleBindingBusinessException(40301, "only the invitee can accept this invite");
        }

        LocalDate today = today();
        LocalDate start = request.startDate() != null ? request.startDate() : today;
        if (start.isAfter(today)) {
            throw new CoupleBindingBusinessException(40004, "startDate cannot be in the future");
        }

        String inviter = binding.getUserId1();
        String invitee = binding.getUserId2();
        String sorted1 = inviter.compareTo(invitee) <= 0 ? inviter : invitee;
        String sorted2 = inviter.compareTo(invitee) <= 0 ? invitee : inviter;
        binding.setUserId1(sorted1);
        binding.setUserId2(sorted2);
        binding.setStartDate(start);
        binding.setStatus(CoupleBindingStatus.ACTIVE);
        int days = RelationshipDaysCalculator.computeDays(start, today);
        binding.setRelationshipDays(days);
        updateById(binding);

        return toInfoResponse(binding, currentUserId, today);
    }

    @Override
    public List<CouplePendingInviteResponse> listPendingInvitesForInvitee(String inviteeUserId) {
        List<CoupleBinding> rows =
                lambdaQuery()
                        .eq(CoupleBinding::getStatus, CoupleBindingStatus.PENDING)
                        .eq(CoupleBinding::getUserId2, inviteeUserId)
                        .orderByDesc(CoupleBinding::getCreatedAt)
                        .list();
        return rows.stream()
                .map(
                        row -> {
                            User inviter = userService.getById(row.getUserId1());
                            if (inviter == null) {
                                throw new CoupleBindingBusinessException(40405, "inviter user not found");
                            }
                            LocalDateTime invitedAt = row.getCreatedAt() != null ? row.getCreatedAt() : LocalDateTime.now();
                            return new CouplePendingInviteResponse(row.getId(), toUserResponse(inviter), invitedAt);
                        })
                .toList();
    }

    @Override
    public long countPendingInvitesForInvitee(String inviteeUserId) {
        return lambdaQuery()
                .eq(CoupleBinding::getStatus, CoupleBindingStatus.PENDING)
                .eq(CoupleBinding::getUserId2, inviteeUserId)
                .count();
    }

    @Override
    public Optional<CoupleInfoResponse> getCoupleInfo(String userId) {
        CoupleBinding binding = findActiveOrFrozenForUser(userId);
        if (binding == null) {
            return Optional.empty();
        }
        LocalDate today = today();
        refreshRelationshipDaysIfNeeded(binding, today);
        return Optional.of(toInfoResponse(binding, userId, today));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStartDate(String userId, LocalDate startDate) {
        CoupleBinding binding = findActiveOrFrozenForUser(userId);
        if (binding == null) {
            throw new CoupleBindingBusinessException(40404, "no active couple binding");
        }
        LocalDate today = today();
        if (startDate.isAfter(today)) {
            throw new CoupleBindingBusinessException(40004, "startDate cannot be in the future");
        }
        binding.setStartDate(startDate);
        binding.setRelationshipDays(RelationshipDaysCalculator.computeDays(startDate, today));
        updateById(binding);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void separate(String userId) {
        CoupleBinding binding = findActiveOrFrozenForUser(userId);
        if (binding == null) {
            throw new CoupleBindingBusinessException(40404, "no active couple binding");
        }
        binding.setStatus(CoupleBindingStatus.SEPARATED);
        updateById(binding);
    }

    @Override
    public Optional<CoupleBinding> findActiveOrFrozenMembership(String userId, String coupleId) {
        CoupleBinding b = getById(coupleId);
        if (b == null) {
            return Optional.empty();
        }
        Integer st = b.getStatus();
        if (!Objects.equals(st, CoupleBindingStatus.ACTIVE) && !Objects.equals(st, CoupleBindingStatus.FROZEN)) {
            return Optional.empty();
        }
        if (!userId.equals(b.getUserId1()) && !userId.equals(b.getUserId2())) {
            return Optional.empty();
        }
        return Optional.of(b);
    }

    private void refreshRelationshipDaysIfNeeded(CoupleBinding binding, LocalDate today) {
        if (binding.getStartDate() == null) {
            return;
        }
        int computed = RelationshipDaysCalculator.computeDays(binding.getStartDate(), today);
        Integer stored = binding.getRelationshipDays();
        if (stored == null || stored != computed) {
            binding.setRelationshipDays(computed);
            updateById(binding);
        }
    }

    private CoupleInfoResponse toInfoResponse(CoupleBinding binding, String viewerUserId, LocalDate today) {
        String partnerId =
                binding.getUserId1().equals(viewerUserId) ? binding.getUserId2() : binding.getUserId1();
        User partner = userService.getById(partnerId);
        if (partner == null) {
            throw new CoupleBindingBusinessException(40405, "partner user not found");
        }
        int days = RelationshipDaysCalculator.computeDays(binding.getStartDate(), today);
        return new CoupleInfoResponse(
                binding.getId(),
                binding.getStartDate(),
                days,
                binding.getStatus(),
                toUserResponse(partner));
    }

    private static UserResponse toUserResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getAvatarUrl(),
                u.getGender(),
                u.getBirthday(),
                u.getStatus(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }

    private CoupleBinding findActiveOrFrozenForUser(String userId) {
        List<CoupleBinding> list =
                lambdaQuery()
                        .and(
                                w ->
                                        w.eq(CoupleBinding::getUserId1, userId)
                                                .or()
                                                .eq(CoupleBinding::getUserId2, userId))
                        .in(
                                CoupleBinding::getStatus,
                                CoupleBindingStatus.ACTIVE,
                                CoupleBindingStatus.FROZEN)
                        .list();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private boolean userHasNonSeparatedBinding(String userId) {
        long count =
                lambdaQuery()
                        .and(
                                w ->
                                        w.eq(CoupleBinding::getUserId1, userId)
                                                .or()
                                                .eq(CoupleBinding::getUserId2, userId))
                        .in(
                                CoupleBinding::getStatus,
                                CoupleBindingStatus.PENDING,
                                CoupleBindingStatus.ACTIVE,
                                CoupleBindingStatus.FROZEN)
                        .count();
        return count > 0;
    }

    private boolean pendingBetween(String a, String b) {
        long count =
                lambdaQuery()
                        .eq(CoupleBinding::getStatus, CoupleBindingStatus.PENDING)
                        .and(
                                w ->
                                        w.and(
                                                        x ->
                                                                x.eq(CoupleBinding::getUserId1, a)
                                                                        .eq(CoupleBinding::getUserId2, b))
                                                .or(
                                                        x ->
                                                                x.eq(CoupleBinding::getUserId1, b)
                                                                        .eq(CoupleBinding::getUserId2, a)))
                        .count();
        return count > 0;
    }

    private static LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
    }
}
