package com.meng.lovespace.user.service;

import com.meng.lovespace.user.dto.MemorialDayCreateRequest;
import com.meng.lovespace.user.dto.MemorialDayResponse;
import com.meng.lovespace.user.dto.MemorialDayUpdateRequest;
import com.meng.lovespace.user.dto.MemorialNextResponse;
import com.meng.lovespace.user.dto.MemorialUpcomingItemResponse;
import java.util.List;

/**
 * 纪念日 CRUD 与倒计时查询。
 */
public interface MemorialDayService {

    MemorialDayResponse create(String userId, MemorialDayCreateRequest req);

    List<MemorialDayResponse> listByCouple(String userId, String coupleId);

    MemorialDayResponse getById(String userId, String id);

    MemorialDayResponse update(String userId, String id, MemorialDayUpdateRequest req);

    void delete(String userId, String id);

    /**
     * 最近的下一个纪念日（全量数据中最近的一次），带剩余毫秒；无数据时返回 {@code memorial=null}。
     */
    MemorialNextResponse getNext(String userId, String coupleId, boolean useCache);

    /** 未来窗口内（默认 7 天，含今天）的纪念日列表，按剩余天数升序。 */
    List<MemorialUpcomingItemResponse> listUpcoming(String userId, String coupleId, boolean useCache);

    /** 预热/刷新某情侣的 Redis 缓存（供定时任务调用）。 */
    void warmCache(String coupleId);
}
