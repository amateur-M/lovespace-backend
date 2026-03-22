package com.meng.lovespace.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meng.lovespace.user.dto.LoveRecordCreateRequest;
import com.meng.lovespace.user.dto.LoveRecordPageResponse;
import com.meng.lovespace.user.dto.LoveRecordResponse;
import com.meng.lovespace.user.dto.LoveRecordUpdateRequest;
import com.meng.lovespace.user.entity.LoveRecord;
import java.time.LocalDate;
import java.util.List;

/**
 * 恋爱时间轴记录：增删改查与回忆推送。
 */
public interface LoveRecordService extends IService<LoveRecord> {

    LoveRecordResponse create(String userId, LoveRecordCreateRequest req);

    /**
     * 分页查询记录；{@code startDate}/{@code endDate} 可选，按 {@code record_date} 闭区间筛选（可只传其一）。
     */
    LoveRecordPageResponse pageRecords(
            String userId, String coupleId, long page, long pageSize, LocalDate startDate, LocalDate endDate);

    LoveRecordResponse getDetail(String userId, String id);

    void update(String userId, String id, LoveRecordUpdateRequest req);

    void delete(String userId, String id);

    /**
     * 回忆推送：优先「历年同月同日」记录，不足时用更多历史记录补齐。
     *
     * @param limit 条数上限（调用方已限制范围）
     */
    List<LoveRecordResponse> memories(String userId, String coupleId, int limit);
}
