package com.meng.lovespace.ai.rag.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RAG 指标收集器，负责记录和输出 Latency 分解指标。
 *
 * <p>当前输出 DEBUG 日志，后续可扩展对接 Micrometer/Prometheus。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagMetricsCollector {

    /**
     * 创建一个新的计时器。
     *
     * @return RagTimer 实例
     */
    public RagTimer startTimer() {
        return new RagTimer();
    }

    /**
     * 记录指标报告。
     *
     * @param timer 计时器
     * @param conversationId 会话 ID（用于日志关联）
     */
    public void record(RagTimer timer, String conversationId) {
        if (timer == null) {
            return;
        }
        try {
            RagMetricsReport report = timer.finish();
            log.debug(report.toLogString(conversationId));
        } catch (Exception e) {
            log.warn("Failed to record RAG metrics: {}", e.getMessage());
        }
    }

    /**
     * 记录指标报告（带额外上下文）。
     *
     * @param timer 计时器
     * @param conversationId 会话 ID
     * @param queryLength query 长度（用于分析）
     * @param retrievedChunkCount 检索到的片段数
     */
    public void record(RagTimer timer, String conversationId, 
                       int queryLength, int retrievedChunkCount) {
        if (timer == null) {
            return;
        }
        try {
            RagMetricsReport report = timer.finish();
            String baseLog = report.toLogString(conversationId);
            log.debug("{} queryLen={} chunks={}", baseLog, queryLength, retrievedChunkCount);
        } catch (Exception e) {
            log.warn("Failed to record RAG metrics: {}", e.getMessage());
        }
    }
}
