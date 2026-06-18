package com.meng.lovespace.user.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meng.lovespace.common.exception.ApiBusinessException;
import com.meng.lovespace.user.couple.CoupleBindingStatus;
import com.meng.lovespace.user.dto.admin.AdminCoupleListItem;
import com.meng.lovespace.user.entity.CoupleBinding;
import com.meng.lovespace.user.entity.User;
import com.meng.lovespace.user.mapper.CoupleBindingMapper;
import com.meng.lovespace.user.service.UserService;
import com.meng.lovespace.user.service.admin.AdminCoupleService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AdminCoupleServiceImpl implements AdminCoupleService {

    private final CoupleBindingMapper coupleBindingMapper;
    private final UserService userService;

    public AdminCoupleServiceImpl(CoupleBindingMapper coupleBindingMapper, UserService userService) {
        this.coupleBindingMapper = coupleBindingMapper;
        this.userService = userService;
    }

    @Override
    public IPage<AdminCoupleListItem> page(Integer status, String keyword, long page, long pageSize) {
        LambdaQueryWrapper<CoupleBinding> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(CoupleBinding::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            Set<String> userIds = findUserIdsByKeyword(kw);
            if (userIds.isEmpty()) {
                return Page.of(page, pageSize);
            }
            qw.and(
                    w ->
                            w.in(CoupleBinding::getUserId1, userIds)
                                    .or()
                                    .in(CoupleBinding::getUserId2, userIds)
                                    .or()
                                    .eq(CoupleBinding::getId, kw));
        }
        qw.orderByDesc(CoupleBinding::getUpdatedAt);
        IPage<CoupleBinding> raw = coupleBindingMapper.selectPage(Page.of(page, pageSize), qw);
        Set<String> allUserIds = new HashSet<>();
        raw.getRecords().forEach(b -> {
            allUserIds.add(b.getUserId1());
            allUserIds.add(b.getUserId2());
        });
        Map<String, User> users =
                allUserIds.isEmpty()
                        ? Map.of()
                        : userService.listByIds(allUserIds).stream()
                                .collect(Collectors.toMap(User::getId, Function.identity()));
        return raw.convert(b -> toItem(b, users));
    }

    @Override
    public AdminCoupleListItem getById(String id) {
        CoupleBinding b = coupleBindingMapper.selectById(id);
        if (b == null) {
            throw new ApiBusinessException(40400, "couple binding not found");
        }
        Map<String, User> users =
                userService.listByIds(List.of(b.getUserId1(), b.getUserId2())).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        return toItem(b, users);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceSeparate(String adminUserId, String coupleId) {
        CoupleBinding b = coupleBindingMapper.selectById(coupleId);
        if (b == null) {
            throw new ApiBusinessException(40400, "couple binding not found");
        }
        b.setStatus(CoupleBindingStatus.SEPARATED);
        coupleBindingMapper.updateById(b);
        log.info("admin.couples.forceSeparate adminUserId={} coupleId={}", adminUserId, coupleId);
    }

    private Set<String> findUserIdsByKeyword(String keyword) {
        LambdaQueryWrapper<User> uq = new LambdaQueryWrapper<>();
        uq.like(User::getPhone, keyword).or().like(User::getUsername, keyword);
        return userService.list(uq).stream().map(User::getId).collect(Collectors.toSet());
    }

    private static AdminCoupleListItem toItem(CoupleBinding b, Map<String, User> users) {
        User u1 = users.get(b.getUserId1());
        User u2 = users.get(b.getUserId2());
        return new AdminCoupleListItem(
                b.getId(),
                b.getUserId1(),
                b.getUserId2(),
                u1 != null ? u1.getPhone() : null,
                u2 != null ? u2.getPhone() : null,
                u1 != null ? u1.getUsername() : null,
                u2 != null ? u2.getUsername() : null,
                b.getStartDate(),
                b.getStatus(),
                b.getCreatedAt(),
                b.getUpdatedAt());
    }
}
