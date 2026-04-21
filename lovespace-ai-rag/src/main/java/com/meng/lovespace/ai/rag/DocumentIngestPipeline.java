package com.meng.lovespace.ai.rag;

import com.meng.lovespace.ai.rag.config.RagAiProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * 将原始文本切分为 {@link Document} 列表（字符滑动窗口，骨架实现；后续可换为 TokenTextSplitter）。
 */
@Component
@RequiredArgsConstructor
public class DocumentIngestPipeline {

    private final RagAiProperties ragAiProperties;

    /**
     * @param text 全文
     * @param baseMetadata 附加元数据（如 source、tags）
     * @return 分片后的文档列表
     */
    public List<Document> splitToDocuments(String text, Map<String, Object> baseMetadata) {
        int chunkSize = Math.max(100, ragAiProperties.getChunkSize());
        int overlap = Math.max(0, ragAiProperties.getChunkOverlap());
        if (overlap >= chunkSize) {
            overlap = chunkSize / 4;
        }
        List<Document> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        int start = 0;
        int len = text.length();
        int part = 0;
        while (start < len) {
            int end = Math.min(start + chunkSize, len);
            String slice = text.substring(start, end);
            String id = UUID.randomUUID().toString();
            Map<String, Object> meta =
                    baseMetadata == null
                            ? Map.of("chunkIndex", part)
                            : new java.util.HashMap<>(baseMetadata);
            meta.put("chunkIndex", part);
            out.add(new Document(id, slice, meta));
            part++;
            if (end >= len) {
                break;
            }
            start = end - overlap;
            if (start < 0) {
                start = 0;
            }
        }
        return out;
    }
}
