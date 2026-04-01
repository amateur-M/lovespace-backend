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
import com.meng.lovespace.user.service.LoveRecordSocialService;
import com.meng.lovespace.user.timeline.LoveMood;
import com.meng.lovespace.user.timeline.LoveRecordVisibility;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link LoveRecordService} 实现：情侣成员校验、可见性 SQL 条件、心情/日期等业务规则。
 */
@Slf4j
@Service
public class LoveRecordServiceImpl extends ServiceImpl<LoveRecordMapper, LoveRecord> implements LoveRecordService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final CoupleBindingService coupleBindingService;
    private final LoveRecordSocialService loveRecordSocialService;

    /**
     * @param coupleBindingService 用于校验 {@code coupleId} 是否对当前用户有效
     * @param loveRecordSocialService 点赞/评论统计与互动
     */
    public LoveRecordServiceImpl(
            CoupleBindingService coupleBindingService, LoveRecordSocialService loveRecordSocialService) {
        this.coupleBindingService = coupleBindingService;
        this.loveRecordSocialService = loveRecordSocialService;
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
        row.setImagesJson(req.imagesJson());
        save(row);
        log.info(
                "loveRecord.created id={} coupleId={} authorId={} recordDate={} mood={}",
                row.getId(),
                row.getCoupleId(),
                row.getAuthorId(),
                row.getRecordDate(),
                row.getMood());
        return loveRecordSocialService.enrichSingle(userId, row);
    }

    @Override
    public LoveRecordPageResponse pageRecords(
            String userId, String coupleId, long page, long pageSize, LocalDate startDate, LocalDate endDate) {
        assertCoupleMember(userId, coupleId);
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new TimelineBusinessException(40054, "startDate must not be after endDate");
        }
        Page<LoveRecord> p = new Page<>(Math.max(1, page), Math.max(1, Math.min(pageSize, 100)));
        LambdaQueryWrapper<LoveRecord> w = visibilityQuery(coupleId, userId);
        if (startDate != null) {
            w.ge(LoveRecord::getRecordDate, startDate);
        }
        if (endDate != null) {
            w.le(LoveRecord::getRecordDate, endDate);
        }
        w.orderByDesc(LoveRecord::getRecordDate).orderByDesc(LoveRecord::getCreatedAt);
        Page<LoveRecord> result = page(p, w);
        List<LoveRecordResponse> list = loveRecordSocialService.enrichWithSocial(userId, result.getRecords());
        log.debug(
                "loveRecord.page userId={} coupleId={} page={} pageSize={} startDate={} endDate={} total={} returned={}",
                userId,
                coupleId,
                page,
                pageSize,
                startDate,
                endDate,
                result.getTotal(),
                list.size());
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
        log.debug("loveRecord.detail id={} viewerId={} coupleId={}", id, userId, row.getCoupleId());
        return loveRecordSocialService.enrichSingle(userId, row);
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
        if (req.imagesJson() != null) {
            row.setImagesJson(req.imagesJson());
        }
        updateById(row);
        log.info("loveRecord.updated id={} editorId={} coupleId={}", id, userId, row.getCoupleId());
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
        log.info("loveRecord.deleted id={} authorId={} coupleId={}", id, userId, row.getCoupleId());
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
        List<LoveRecord> ordered = new ArrayList<>();
        for (LoveRecord r : first) {
            if (seen.add(r.getId())) {
                ordered.add(r);
            }
        }

        if (ordered.size() < cap) {
            int need = cap - ordered.size();
            LambdaQueryWrapper<LoveRecord> more = visibilityQuery(coupleId, userId);
            if (!seen.isEmpty()) {
                more.notIn(LoveRecord::getId, seen);
            }
            more.orderByDesc(LoveRecord::getRecordDate).last("LIMIT " + need);
            List<LoveRecord> rest = list(more);
            for (LoveRecord r : rest) {
                if (seen.add(r.getId())) {
                    ordered.add(r);
                }
                if (ordered.size() >= cap) {
                    break;
                }
            }
        }

        List<LoveRecordResponse> out = loveRecordSocialService.enrichWithSocial(userId, ordered);
        log.debug("loveRecord.memories userId={} coupleId={} limit={} returned={}", userId, coupleId, cap, out.size());
        return out;
    }

    /** 校验当前用户是否为该情侣绑定成员且状态为交往/冻结。 */
    private void assertCoupleMember(String userId, String coupleId) {
        coupleBindingService
                .findActiveOrFrozenMembership(userId, coupleId)
                .orElseThrow(() -> new TimelineBusinessException(40351, "forbidden or invalid couple"));
    }

    /** 构造「情侣可见 + 本人可见的仅自己记录」查询条件。 */
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

    /** 详情接口在已是情侣成员前提下，再判断单条记录的可见性。 */
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

    /** 校验心情标签是否在 {@link LoveMood#ALLOWED} 内。 */
    private static void validateMood(String mood) {
        if (!LoveMood.isAllowed(mood)) {
            throw new TimelineBusinessException(40052, "invalid mood, allowed: " + LoveMood.ALLOWED);
        }
    }

    /** 校验可见性取值为 {@link LoveRecordVisibility} 定义。 */
    private static void validateVisibility(int v) {
        if (v != LoveRecordVisibility.SELF && v != LoveRecordVisibility.COUPLE) {
            throw new TimelineBusinessException(40053, "invalid visibility");
        }
    }

}
