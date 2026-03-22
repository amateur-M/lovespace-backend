package com.meng.lovespace.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meng.lovespace.user.dto.LoveRecordCreateRequest;
import com.meng.lovespace.user.dto.LoveRecordPageResponse;
import com.meng.lovespace.user.dto.LoveRecordResponse;
import com.meng.lovespace.user.dto.LoveRecordUpdateRequest;
import com.meng.lovespace.user.entity.LoveRecord;
import com.meng.lovespace.user.exception.TimelineBusinessException;
import com.meng.lovespace.user.mapper.LoveRecordMapper;
import com.meng.lovespace.user.service.CoupleBindingService;
import com.meng.lovespace.user.service.LoveRecordService;
import com.meng.lovespace.user.timeline.LoveMood;
import com.meng.lovespace.user.timeline.LoveRecordVisibility;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoveRecordServiceImpl extends ServiceImpl<LoveRecordMapper, LoveRecord> implements LoveRecordService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final CoupleBindingService coupleBindingService;

    public LoveRecordServiceImpl(CoupleBindingService coupleBindingService) {
        this.coupleBindingService = coupleBindingService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoveRecordResponse create(String userId, LoveRecordCreateRequest req) {
        assertCoupleMember(userId, req.coupleId());
        validateMood(req.mood());
        validateVisibility(req.visibility());
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (req.recordDate().isAfter(today)) {
            throw new TimelineBusinessException(40051, "recordDate cannot be in the future");
        }

        LoveRecord row = new LoveRecord();
        row.setCoupleId(req.coupleId());
        row.setAuthorId(userId);
        row.setRecordDate(req.recordDate());
        row.setContent(req.content());
        row.setMood(req.mood());
        row.setLocationJson(req.locationJson());
        row.setVisibility(req.visibility());
        row.setTagsJson(req.tagsJson());
        save(row);
        return LoveRecordServiceImpl.toResponse(row);
    }

    @Override
    public LoveRecordPageResponse pageRecords(String userId, String coupleId, long page, long pageSize) {
        assertCoupleMember(userId, coupleId);
        Page<LoveRecord> p = new Page<>(Math.max(1, page), Math.max(1, Math.min(pageSize, 100)));
        LambdaQueryWrapper<LoveRecord> w = visibilityQuery(coupleId, userId);
        w.orderByDesc(LoveRecord::getRecordDate).orderByDesc(LoveRecord::getCreatedAt);
        Page<LoveRecord> result = page(p, w);
        List<LoveRecordResponse> list = result.getRecords().stream().map(LoveRecordServiceImpl::toResponse).toList();
        return new LoveRecordPageResponse(result.getTotal(), result.getCurrent(), result.getSize(), list);
    }

    @Override
    public LoveRecordResponse getDetail(String userId, String id) {
        LoveRecord row = getById(id);
        if (row == null) {
            throw new TimelineBusinessException(40452, "record not found");
        }
        assertCoupleMember(userId, row.getCoupleId());
        if (!canView(row, userId)) {
            throw new TimelineBusinessException(40352, "forbidden to view this record");
        }
        return LoveRecordServiceImpl.toResponse(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String userId, String id, LoveRecordUpdateRequest req) {
        LoveRecord row = getById(id);
        if (row == null) {
            throw new TimelineBusinessException(40452, "record not found");
        }
        assertCoupleMember(userId, row.getCoupleId());
        if (!userId.equals(row.getAuthorId())) {
            throw new TimelineBusinessException(40353, "only author can update");
        }

        if (req.recordDate() != null) {
            LocalDate today = LocalDate.now(BUSINESS_ZONE);
            if (req.recordDate().isAfter(today)) {
                throw new TimelineBusinessException(40051, "recordDate cannot be in the future");
            }
            row.setRecordDate(req.recordDate());
        }
        if (req.content() != null) {
            row.setContent(req.content());
        }
        if (req.mood() != null) {
            validateMood(req.mood());
            row.setMood(req.mood());
        }
        if (req.locationJson() != null) {
            row.setLocationJson(req.locationJson());
        }
        if (req.visibility() != null) {
            validateVisibility(req.visibility());
            row.setVisibility(req.visibility());
        }
        if (req.tagsJson() != null) {
            row.setTagsJson(req.tagsJson());
        }
        updateById(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String userId, String id) {
        LoveRecord row = getById(id);
        if (row == null) {
            throw new TimelineBusinessException(40452, "record not found");
        }
        assertCoupleMember(userId, row.getCoupleId());
        if (!userId.equals(row.getAuthorId())) {
            throw new TimelineBusinessException(40354, "only author can delete");
        }
        removeById(id);
    }

    @Override
    public List<LoveRecordResponse> memories(String userId, String coupleId, int limit) {
        assertCoupleMember(userId, coupleId);
        int cap = Math.max(1, Math.min(limit, 30));
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        String md = String.format("%02d-%02d", today.getMonthValue(), today.getDayOfMonth());

        LambdaQueryWrapper<LoveRecord> onThisDay = visibilityQuery(coupleId, userId);
        onThisDay
                .apply("DATE_FORMAT(record_date, '%m-%d') = {0}", md)
                .apply("YEAR(record_date) < {0}", today.getYear())
                .orderByDesc(LoveRecord::getRecordDate)
                .last("LIMIT " + cap);

        List<LoveRecord> first = list(onThisDay);
        Set<String> seen = new LinkedHashSet<>();
        List<LoveRecordResponse> out = new ArrayList<>();
        for (LoveRecord r : first) {
            if (seen.add(r.getId())) {
                out.add(LoveRecordServiceImpl.toResponse(r));
            }
        }

        if (out.size() < cap) {
            int need = cap - out.size();
            LambdaQueryWrapper<LoveRecord> more = visibilityQuery(coupleId, userId);
            if (!seen.isEmpty()) {
                more.notIn(LoveRecord::getId, seen);
            }
            more.orderByDesc(LoveRecord::getRecordDate).last("LIMIT " + need);
            List<LoveRecord> rest = list(more);
            for (LoveRecord r : rest) {
                if (seen.add(r.getId())) {
                    out.add(LoveRecordServiceImpl.toResponse(r));
                }
                if (out.size() >= cap) {
                    break;
                }
            }
        }

        return out;
    }

    private void assertCoupleMember(String userId, String coupleId) {
        coupleBindingService
                .findActiveOrFrozenMembership(userId, coupleId)
                .orElseThrow(() -> new TimelineBusinessException(40351, "forbidden or invalid couple"));
    }

    private static LambdaQueryWrapper<LoveRecord> visibilityQuery(String coupleId, String viewerId) {
        LambdaQueryWrapper<LoveRecord> w = new LambdaQueryWrapper<>();
        w.eq(LoveRecord::getCoupleId, coupleId)
                .and(
                        x ->
                                x.eq(LoveRecord::getVisibility, LoveRecordVisibility.COUPLE)
                                        .or(
                                                y ->
                                                        y.eq(LoveRecord::getVisibility, LoveRecordVisibility.SELF)
                                                                .eq(LoveRecord::getAuthorId, viewerId)));
        return w;
    }

    private static boolean canView(LoveRecord row, String viewerId) {
        if (row.getVisibility() == null) {
            return false;
        }
        if (Objects.equals(row.getVisibility(), LoveRecordVisibility.COUPLE)) {
            return true;
        }
        return Objects.equals(row.getVisibility(), LoveRecordVisibility.SELF)
                && Objects.equals(row.getAuthorId(), viewerId);
    }

    private static void validateMood(String mood) {
        if (!LoveMood.isAllowed(mood)) {
            throw new TimelineBusinessException(40052, "invalid mood, allowed: " + LoveMood.ALLOWED);
        }
    }

    private static void validateVisibility(int v) {
        if (v != LoveRecordVisibility.SELF && v != LoveRecordVisibility.COUPLE) {
            throw new TimelineBusinessException(40053, "invalid visibility");
        }
    }

    private static LoveRecordResponse toResponse(LoveRecord r) {
        return new LoveRecordResponse(
                r.getId(),
                r.getCoupleId(),
                r.getAuthorId(),
                r.getRecordDate(),
                r.getContent(),
                r.getMood(),
                r.getLocationJson(),
                r.getVisibility(),
                r.getTagsJson(),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
