package com.meng.lovespace.user.task;

import com.meng.lovespace.user.mapper.MemorialDayMapper;
import com.meng.lovespace.user.service.MemorialDayService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时预热纪念日 Redis 缓存（减轻首次访问冷启动；写操作仍会主动失效缓存）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemorialDayCacheRefreshTask {

    private final MemorialDayMapper memorialDayMapper;
    private final MemorialDayService memorialDayService;

    /** 每天 03:15 全量预热（可按需改 cron）。 */
    @Scheduled(cron = "0 15 3 * * ?")
    public void warmAll() {
        try {
            List<String> coupleIds = memorialDayMapper.selectDistinctCoupleIds();
            for (String coupleId : coupleIds) {
                try {
                    memorialDayService.warmCache(coupleId);
                } catch (Exception e) {
                    log.warn("memorial cache warm failed coupleId={} msg={}", coupleId, e.getMessage());
                }
            }
            log.debug("memorial cache warm finished couples={}", coupleIds.size());
        } catch (Exception e) {
            log.warn("memorial cache warm batch failed: {}", e.getMessage());
        }
    }
}
