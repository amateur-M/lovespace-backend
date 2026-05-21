package com.meng.lovespace.ai.rag;

import com.meng.lovespace.ai.rag.config.RagAiProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

/**
 * 将原始文本切分为 {@link Document} 列表。
 *
 * <p>支持两种策略（通过 {@code lovespace.ai.rag.chunk-strategy} 配置）：
 * <ul>
 *   <li><b>token</b>（默认推荐）：使用 Spring AI {@link TokenTextSplitter}，基于 token 切分，语义更连贯，适合大模型上下文。</li>
 *   <li><b>character</b>：兼容旧版字符级滑动窗口。</li>
 * </ul>
 * 开启 {@code deduplicate-on-ingest} 时会基于内容 SHA-256 自动去重，避免重复向量。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIngestPipeline {

    private final RagAiProperties ragAiProperties;

    /**
     * @param text 全文
     * @param baseMetadata 附加元数据（如 source、tags、ownerUserId）
     * @return 分片后的文档列表（已去重）
     */
    public List<Document> splitToDocuments(String text, Map<String, Object> baseMetadata) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        int chunkSize = Math.max(200, ragAiProperties.getChunkSize());
        int overlap = Math.max(0, ragAiProperties.getChunkOverlap());
        if (overlap >= chunkSize) {
            overlap = chunkSize / 5; // 更合理的默认 overlap
        }

        List<Document> chunks;
        String strategy = ragAiProperties.getChunkStrategy();

        if ("token".equalsIgnoreCase(strategy)) {
            // Token 级别切分（推荐）
            TokenTextSplitter splitter = new TokenTextSplitter(
                    chunkSize,           // chunkSize（token 数近似）
                    overlap,             // chunkOverlap
                    5,                   // minChunkSizeChars
                    2000,                // maxChunkSizeChars 保护
                    true                 // keepSeparator
            );
            Document doc = new Document(text, baseMetadata != null ? baseMetadata : Map.of());
            chunks = splitter.split(doc);
        } else {
            // 兼容旧版字符滑动窗口
            chunks = characterBasedSplit(text, chunkSize, overlap, baseMetadata);
        }

        // 入库前内容去重（基于 SHA-256）
        if (ragAiProperties.isDeduplicateOnIngest()) {
            int before = chunks.size();
            chunks = deduplicateByContentHash(chunks);
            if (chunks.size() < before) {
                log.info("DocumentIngestPipeline: 去重 {} -> {} chunks", before, chunks.size());
            }
        }

        log.info("DocumentIngestPipeline: strategy={}, chunkSize={}, overlap={}, chunks={}",
                strategy, chunkSize, overlap, chunks.size());
        return chunks;
    }

    /**
     * 旧版字符级滑动窗口实现（向后兼容）。
     */
    private List<Document> characterBasedSplit(String text, int chunkSize, int overlap,
                                               Map<String, Object> baseMetadata) {
        List<Document> out = new ArrayList<>();
        int start = 0;
        int len = text.length();
        int part = 0;
        while (start < len) {
            int end = Math.min(start + chunkSize, len);
            String slice = text.substring(start, end);
            Map<String, Object> meta =
                    baseMetadata == null
                            ? new java.util.HashMap<>()
                            : new java.util.HashMap<>(baseMetadata);
            meta.put("chunkIndex", part);
            out.add(new Document(slice, meta)); // TokenTextSplitter 会自动生成 ID，这里也简化
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

    /**
     * 基于内容 SHA-256 的去重。
     */
    private List<Document> deduplicateByContentHash(List<Document> docs) {
        Map<String, Document> unique = new LinkedHashMap<>();
        for (Document doc : docs) {
            if (doc == null || doc.getText() == null) continue;
            String hash = sha256(doc.getText());
            unique.putIfAbsent(hash, doc);
        }
        return new ArrayList<>(unique.values());
    }

    private String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
