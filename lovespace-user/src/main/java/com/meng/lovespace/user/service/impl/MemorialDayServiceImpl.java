package com.meng.lovespace.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meng.lovespace.user.config.LovespaceMemorialProperties;
import com.meng.lovespace.user.dto.MemorialDayCreateRequest;
import com.meng.lovespace.user.dto.MemorialDayResponse;
import com.meng.lovespace.user.dto.MemorialDayUpdateRequest;
import com.meng.lovespace.user.dto.MemorialNextResponse;
import com.meng.lovespace.user.dto.MemorialUpcomingItemResponse;
import com.meng.lovespace.user.entity.MemorialDay;
import com.meng.lovespace.user.exception.MemorialDayBusinessException;
import com.meng.lovespace.user.mapper.MemorialDayMapper;
import com.meng.lovespace.user.service.CoupleBindingService;
import com.meng.lovespace.user.service.MemorialDayRedisCache;
import com.meng.lovespace.user.service.MemorialDayService;
import com.meng.lovespace.user.util.MemorialCountdownCalculator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * {@link MemorialDayService} 实现。
 */
@Slf4j
@Service
public class MemorialDayServiceImpl extends ServiceImpl<MemorialDayMapper, MemorialDay> implements MemorialDayService {

    private static final int NOT_FOUND = 40490;
    private static final int FORBIDDEN = 40390;

    private final CoupleBindingService coupleBindingService;
    private final MemorialDayRedisCache memorialDayRedisCache;
    private final LovespaceMemorialProperties memorialProperties;

    public MemorialDayServiceImpl(
            CoupleBindingService coupleBindingService,
            MemorialDayRedisCache memorialDayRedisCache,
            LovespaceMemorialProperties memorialProperties) {
        this.coupleBindingService = coupleBindingService;
        this.memorialDayRedisCache = memorialDayRedisCache;
        this.memorialProperties = memorialProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemorialDayResponse create(String userId, MemorialDayCreateRequest req) {
        requireMembership(userId, req.coupleId());
        validateName(req.name());
        MemorialDay e = new MemorialDay();
        e.setCoupleId(req.coupleId().trim());
        e.setUserId(userId);
        e.setName(req.name().trim());
        e.setDescription(trimToNull(req.description()));
        e.setMemorialDate(req.memorialDate());
        save(e);
        log.info("memorial.created id={} coupleId={} userId={}", e.getId(), e.getCoupleId(), userId);
        memorialDayRedisCache.evictCouple(req.coupleId().trim());
        return toResponse(e);
    }

    @Override
    public List<MemorialDayResponse> listByCouple(String userId, String coupleId) {
        requireMembership(userId, coupleId);
        List<MemorialDay> list =
                lambdaQuery().eq(MemorialDay::getCoupleId, coupleId).orderByDesc(MemorialDay::getCreatedAt).list();
        return list.stream().map(MemorialDayServiceImpl::toResponse).toList();
    }

    @Override
    public MemorialDayResponse getById(String userId, String id) {
        MemorialDay e = getOpt(id).orElseThrow(() -> new MemorialDayBusinessException(NOT_FOUND, "memorial day not found"));
        requireMembership(userId, e.getCoupleId());
        return toResponse(e);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemorialDayResponse update(String userId, String id, MemorialDayUpdateRequest req) {
        MemorialDay e = getOpt(id).orElseThrow(() -> new MemorialDayBusinessException(NOT_FOUND, "memorial day not found"));
        requireMembership(userId, e.getCoupleId());
        validateName(req.name());
        e.setName(req.name().trim());
        e.setDescription(trimToNull(req.description()));
        e.setMemorialDate(req.memorialDate());
        updateById(e);
        log.info("memorial.updated id={} coupleId={}", id, e.getCoupleId());
        memorialDayRedisCache.evictCouple(e.getCoupleId());
        return toResponse(e);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String userId, String id) {
        MemorialDay e = getOpt(id).orElseThrow(() -> new MemorialDayBusinessException(NOT_FOUND, "memorial day not found"));
        requireMembership(userId, e.getCoupleId());
        removeById(id);
        log.info("memorial.deleted id={} coupleId={}", id, e.getCoupleId());
        memorialDayRedisCache.evictCouple(e.getCoupleId());
    }

    @Override
    public MemorialNextResponse getNext(String userId, String coupleId, boolean useCache) {
        requireMembership(userId, coupleId);
        ZoneId zone = ZoneId.of(memorialProperties.getZoneId());
        if (useCache) {
            MemorialNextResponse cached = memorialDayRedisCache.getNext(coupleId);
            if (cached != null && cached.memorial() != null) {
                return refreshNextTiming(cached, zone);
            }
        }
        MemorialNextResponse computed = computeNext(coupleId, zone);
        memorialDayRedisCache.putNext(coupleId, computed);
        return computed;
    }

    @Override
    public List<MemorialUpcomingItemResponse> listUpcoming(String userId, String coupleId, boolean useCache) {
        requireMembership(userId, coupleId);
        ZoneId zone = ZoneId.of(memorialProperties.getZoneId());
        if (useCache) {
            List<MemorialUpcomingItemResponse> cached = memorialDayRedisCache.getUpcoming(coupleId);
            if (cached != null) {
                return cached.stream().map(i -> refreshUpcomingItem(i, zone)).toList();
            }
        }
        List<MemorialUpcomingItemResponse> built = buildUpcoming(coupleId, zone);
        memorialDayRedisCache.putUpcoming(coupleId, built);
        return built;
    }

    @Override
    public void warmCache(String coupleId) {
        ZoneId zone = ZoneId.of(memorialProperties.getZoneId());
        MemorialNextResponse next = computeNext(coupleId, zone);
        memorialDayRedisCache.putNext(coupleId, next);
        memorialDayRedisCache.putUpcoming(coupleId, buildUpcoming(coupleId, zone));
    }

    private MemorialNextResponse computeNext(String coupleId, ZoneId zone) {
        List<MemorialDay> all = lambdaQuery().eq(MemorialDay::getCoupleId, coupleId).list();
        if (all.isEmpty()) {
            return new MemorialNextResponse(null, null, 0, 0, false);
        }
        return all.stream()
                .map(m -> buildNextForEntity(m, zone))
                .min(Comparator.comparingLong(MemorialNextResponse::daysUntil)
                        .thenComparingLong(MemorialNextResponse::millisecondsUntilNext)
                        .thenComparing(x -> x.memorial().id()))
                .orElse(new MemorialNextResponse(null, null, 0, 0, false));
    }

    private MemorialNextResponse buildNextForEntity(MemorialDay m, ZoneId zone) {
        LocalDate md = m.getMemorialDate();
        LocalDate next = MemorialCountdownCalculator.nextOccurrenceDate(
                java.time.MonthDay.from(md), zone);
        long days = MemorialCountdownCalculator.daysUntilNext(md, zone);
        long ms = MemorialCountdownCalculator.millisecondsUntilNext(md, zone);
        boolean today = MemorialCountdownCalculator.isToday(md, zone);
        return new MemorialNextResponse(toResponse(m), next, days, ms, today);
    }

    private MemorialNextResponse refreshNextTiming(MemorialNextResponse cached, ZoneId zone) {
        if (cached.memorial() == null) {
            return cached;
        }
        LocalDate md = cached.memorial().memorialDate();
        LocalDate next = MemorialCountdownCalculator.nextOccurrenceDate(java.time.MonthDay.from(md), zone);
        long days = MemorialCountdownCalculator.daysUntilNext(md, zone);
        long ms = MemorialCountdownCalculator.millisecondsUntilNext(md, zone);
        boolean today = MemorialCountdownCalculator.isToday(md, zone);
        return new MemorialNextResponse(cached.memorial(), next, days, ms, today);
    }

    private List<MemorialUpcomingItemResponse> buildUpcoming(String coupleId, ZoneId zone) {
        int window = Math.max(1, memorialProperties.getUpcomingWindowDays());
        List<MemorialDay> all = lambdaQuery().eq(MemorialDay::getCoupleId, coupleId).list();
        List<MemorialUpcomingItemResponse> out = new ArrayList<>();
        for (MemorialDay m : all) {
            long days = MemorialCountdownCalculator.daysUntilNext(m.getMemorialDate(), zone);
            if (days < 0 || days >= window) {
                continue;
            }
            LocalDate next = MemorialCountdownCalculator.nextOccurrenceDate(
                    java.time.MonthDay.from(m.getMemorialDate()), zone);
            long ms = MemorialCountdownCalculator.millisecondsUntilNext(m.getMemorialDate(), zone);
            boolean today = MemorialCountdownCalculator.isToday(m.getMemorialDate(), zone);
            out.add(new MemorialUpcomingItemResponse(
                    m.getId(), m.getName(), m.getMemorialDate(), next, days, ms, today));
        }
        out.sort(Comparator.comparingLong(MemorialUpcomingItemResponse::daysUntil)
                .thenComparingLong(MemorialUpcomingItemResponse::millisecondsUntilNext)
                .thenComparing(MemorialUpcomingItemResponse::id));
        return out;
    }

    private MemorialUpcomingItemResponse refreshUpcomingItem(MemorialUpcomingItemResponse i, ZoneId zone) {
        long days = MemorialCountdownCalculator.daysUntilNext(i.memorialDate(), zone);
        LocalDate next = MemorialCountdownCalculator.nextOccurrenceDate(
                java.time.MonthDay.from(i.memorialDate()), zone);
        long ms = MemorialCountdownCalculator.millisecondsUntilNext(i.memorialDate(), zone);
        boolean today = MemorialCountdownCalculator.isToday(i.memorialDate(), zone);
        return new MemorialUpcomingItemResponse(
                i.id(), i.name(), i.memorialDate(), next, days, ms, today);
    }

    private java.util.Optional<MemorialDay> getOpt(String id) {
        return java.util.Optional.ofNullable(getById(id));
    }

    private void requireMembership(String userId, String coupleId) {
        coupleBindingService
                .findActiveOrFrozenMembership(userId, coupleId)
                .orElseThrow(() -> new MemorialDayBusinessException(FORBIDDEN, "forbidden or invalid couple"));
    }

    private static void validateName(String name) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(name.trim())) {
            throw new MemorialDayBusinessException(40091, "name is required");
        }
    }

    private static String trimToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static MemorialDayResponse toResponse(MemorialDay e) {
        return new MemorialDayResponse(
                e.getId(),
                e.getCoupleId(),
                e.getUserId(),
                e.getName(),
                e.getDescription(),
                e.getMemorialDate(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
