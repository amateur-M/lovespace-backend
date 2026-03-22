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
 * 恋爱时间轴记录领域服务：创建、分页、详情、更新、删除与回忆推送。
 *
 * <p>所有操作均会校验当前用户是否为 {@code coupleId} 对应情侣（交往/冻结）成员；列表与详情另按 {@code visibility} 过滤。
 */
public interface LoveRecordService extends IService<LoveRecord> {

    /**
     * 创建一条记录，作者为 {@code userId}。
     *
     * @param userId 当前登录用户
     * @param req 情侣 ID、记录日、内容、心情、可见性等
     * @return 新建记录视图
     * @throws com.meng.lovespace.user.exception.TimelineBusinessException 校验失败
     */
    LoveRecordResponse create(String userId, LoveRecordCreateRequest req);

    /**
     * 分页查询当前用户在 {@code coupleId} 下可见的记录。
     *
     * @param startDate 可选，{@code record_date} 下限（含）
     * @param endDate 可选，{@code record_date} 上限（含）；若均传入则须 {@code startDate <= endDate}
     */
    LoveRecordPageResponse pageRecords(
            String userId, String coupleId, long page, long pageSize, LocalDate startDate, LocalDate endDate);

    /**
     * 按主键查询详情（含情侣成员与可见性校验）。
     */
    LoveRecordResponse getDetail(String userId, String id);

    /**
     * 部分更新记录，仅作者可操作。
     */
    void update(String userId, String id, LoveRecordUpdateRequest req);

    /**
     * 删除记录，仅作者可操作。
     */
    void delete(String userId, String id);

    /**
     * 回忆推送：优先「历年同月同日」记录，不足时用更多历史记录补齐。
     *
     * @param limit 条数上限（调用方已限制范围）
     */
    List<LoveRecordResponse> memories(String userId, String coupleId, int limit);
}
