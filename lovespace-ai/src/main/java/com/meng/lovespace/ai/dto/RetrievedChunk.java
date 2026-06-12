package com.meng.lovespace.ai.dto;

import com.meng.lovespace.ai.rag.retrieve.RagSimilarityFilter;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.document.Document;

/**
 * 检索到的知识片段，用于前端可视化展示引用来源。
 */
@Data
@Builder
public class RetrievedChunk {

    /** 片段 ID（对应 Milvus 中的文档 ID） */
    private String id;

    /** 片段内容预览（前 N 个字符） */
    private String textPreview;

    /** 相似度分数（0-1 之间，越高越相关） */
    private Double score;

    /** 来源信息（如文档标题、URL 等） */
    private String source;

    /** 元数据（可选，包含额外信息） */
    private java.util.Map<String, Object> metadata;

    /**
     * 从 Spring AI Document 创建 RetrievedChunk。
     *
     * @param doc Spring AI Document
     * @param previewLength 预览长度
     * @return RetrievedChunk
     */
    public static RetrievedChunk fromDocument(Document doc, int previewLength) {
        if (doc == null) {
            return null;
        }
        String text = doc.getText();
        String preview = text != null && text.length() > previewLength
                ? text.substring(0, previewLength) + "..."
                : text;

        RetrievedChunkBuilder builder = RetrievedChunk.builder()
                .id(doc.getId())
                .textPreview(preview);

        if (doc.getMetadata() != null) {
            Double score = RagSimilarityFilter.scoreFromDocument(doc);
            if (score != null) {
                builder.score(score);
            }
            // 提取来源信息
            Object sourceObj = doc.getMetadata().get("title");
            if (sourceObj != null) {
                builder.source(sourceObj.toString());
            }
            builder.metadata(new java.util.HashMap<>(doc.getMetadata()));
        }

        return builder.build();
    }
}
