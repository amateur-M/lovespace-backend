package com.meng.lovespace.ai.rag.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;

/**
 * RAG 流程计时器，记录各阶段耗时。
 *
 * <p>使用 System.nanoTime() 获取高精度时间戳。
 */
public class RagTimer {

    private final long startNanos;
    private final Map<RagPhase, Long> phaseStartNanos = new HashMap<>();
    private final Map<RagPhase, Long> phaseEndNanos = new HashMap<>();

    @Getter
    private volatile boolean embeddingCacheHit = false;

    @Getter
    private volatile boolean llmFirstByteRecorded = false;

    private final AtomicBoolean llmFirstByteFlag = new AtomicBoolean(false);

    public RagTimer() {
        this.startNanos = System.nanoTime();
        this.phaseStartNanos.put(RagPhase.START, startNanos);
    }

    /**
     * 标记进入新阶段。
     *
     * @param phase 阶段
     */
    public void phase(RagPhase phase) {
        long now = System.nanoTime();
        phaseStartNanos.put(phase, now);
    }

    /**
     * 标记 Embedding 阶段完成。
     *
     * @param cacheHit 是否命中缓存
     */
    public void markEmbeddingDone(boolean cacheHit) {
        this.embeddingCacheHit = cacheHit;
        phaseEndNanos.put(RagPhase.EMBEDDING, System.nanoTime());
    }

    /**
     * 标记检索阶段完成。
     */
    public void markRetrieveDone() {
        phaseEndNanos.put(RagPhase.RETRIEVE, System.nanoTime());
    }

    /**
     * 标记 Prompt 构建完成。
     */
    public void markPromptBuildDone() {
        phaseEndNanos.put(RagPhase.PROMPT_BUILD, System.nanoTime());
    }

    /**
     * 标记收到 LLM 第一个字节（仅首次调用有效）。
     */
    public void markLlmFirstByte() {
        if (llmFirstByteFlag.compareAndSet(false, true)) {
            this.llmFirstByteRecorded = true;
            phaseEndNanos.put(RagPhase.LLM_FIRST_BYTE, System.nanoTime());
        }
    }

    /**
     * 标记是否已记录首字节（供外部查询）。
     *
     * @return true 如果还未记录首字节
     */
    public boolean isFirstBytePending() {
        return !llmFirstByteFlag.get();
    }

    /**
     * 标记 LLM 完整生成完成。
     */
    public void markLlmTotalDone() {
        phaseEndNanos.put(RagPhase.LLM_TOTAL, System.nanoTime());
    }

    /**
     * 标记持久化完成。
     */
    public void markPersistDone() {
        phaseEndNanos.put(RagPhase.PERSIST, System.nanoTime());
    }

    /**
     * 结束计时并生成报告。
     *
     * @return 指标报告
     */
    public RagMetricsReport finish() {
        long endNanos = System.nanoTime();
        phaseEndNanos.put(RagPhase.END, endNanos);
        return new RagMetricsReport(
            nanosToMillis(endNanos - startNanos),
            calculatePhaseMillis(RagPhase.EMBEDDING),
            calculatePhaseMillis(RagPhase.RETRIEVE),
            calculatePhaseMillis(RagPhase.PROMPT_BUILD),
            calculatePhaseMillis(RagPhase.LLM_FIRST_BYTE),
            calculatePhaseMillis(RagPhase.LLM_TOTAL),
            calculatePhaseMillis(RagPhase.PERSIST),
            embeddingCacheHit
        );
    }

    private Long calculatePhaseMillis(RagPhase phase) {
        Long start = phaseStartNanos.get(phase);
        Long end = phaseEndNanos.get(phase);
        if (start == null || end == null) {
            return null;
        }
        return nanosToMillis(end - start);
    }

    private static long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }
}
