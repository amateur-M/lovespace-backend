package com.meng.lovespace.ai.rag.compress;

import com.meng.lovespace.ai.dto.ChatTurn;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * Prompt 压缩器，减少 Token 成本。
 *
 * <p>功能：
 * <ul>
 *   <li>检索片段去重</li>
 *   <li>上下文长度限制</li>
 *   <li>历史对话精简（超长时保留最近 N 轮）</li>
 * </ul>
 */
@Slf4j
@Component
public class PromptCompressor {

    /** 默认最大上下文长度（字符数），约对应 2000-3000 tokens */
    private static final int DEFAULT_MAX_CONTEXT_LENGTH = 8000;

    /** 默认最大保留历史轮数 */
    private static final int DEFAULT_MAX_HISTORY_PAIRS = 10;

    private final DocumentDeduplicator deduplicator;
    private final int maxContextLength;
    private final int maxHistoryPairs;

    public PromptCompressor() {
        this(new DocumentDeduplicator(), DEFAULT_MAX_CONTEXT_LENGTH, DEFAULT_MAX_HISTORY_PAIRS);
    }

    public PromptCompressor(DocumentDeduplicator deduplicator, 
                           int maxContextLength, 
                           int maxHistoryPairs) {
        this.deduplicator = deduplicator;
        this.maxContextLength = maxContextLength;
        this.maxHistoryPairs = maxHistoryPairs;
    }

    /**
     * 压缩检索片段。
     *
     * @param docs 原始检索结果
     * @return 压缩后的结果
     */
    public List<Document> compressRetrievedDocs(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return docs;
        }

        // 1. 去重
        List<Document> deduped = deduplicator.deduplicate(docs);

        // 2. 长度限制
        return applyLengthLimit(deduped, maxContextLength);
    }

    /**
     * 压缩历史对话。
     *
     * @param history 原始历史对话
     * @return 压缩后的历史（最多保留 maxHistoryPairs 轮）
     */
    public List<ChatTurn> compressHistory(List<ChatTurn> history) {
        if (history == null || history.size() <= maxHistoryPairs * 2) {
            return history;
        }

        // 保留最近 N 轮
        int startIndex = history.size() - maxHistoryPairs * 2;
        List<ChatTurn> compressed = history.subList(startIndex, history.size());
        log.debug("History compressed from {} to {} turns", history.size() / 2, compressed.size() / 2);
        return compressed;
    }

    /**
     * 构建最终的系统 Prompt。
     *
     * @param systemPrefix 系统前缀（指令）
     * @param context 检索上下文
     * @return 组合后的系统 Prompt
     */
    public String buildSystemPrompt(String systemPrefix, String context) {
        StringBuilder sb = new StringBuilder(systemPrefix);
        sb.append("\n\n【检索到的上下文】\n");
        
        // 如果上下文超长，截断
        if (context != null && context.length() > maxContextLength) {
            context = context.substring(0, maxContextLength) + "\n...（内容已截断）";
            log.debug("Context truncated to {} chars", maxContextLength);
        }
        
        sb.append(context);
        return sb.toString();
    }

    /**
     * 应用长度限制。
     */
    private List<Document> applyLengthLimit(List<Document> docs, int maxLength) {
        int currentLength = 0;
        List<Document> result = new ArrayList<>();

        for (Document doc : docs) {
            String text = doc.getText();
            if (text == null) continue;

            // 预估长度（字符数）
            int estimatedLength = text.length();
            
            if (currentLength + estimatedLength > maxLength && !result.isEmpty()) {
                // 已超限，停止添加
                log.debug("Length limit reached, kept {} docs", result.size());
                break;
            }

            result.add(doc);
            currentLength += estimatedLength;
        }

        return result;
    }

    /**
     * 压缩统计信息。
     */
    public record CompressionStats(
        int originalDocCount,
        int dedupedDocCount,
        int finalDocCount,
        int originalHistoryTurns,
        int compressedHistoryTurns
    ) {}
}
