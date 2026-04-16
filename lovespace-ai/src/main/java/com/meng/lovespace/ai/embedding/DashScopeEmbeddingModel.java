package com.meng.lovespace.ai.embedding;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 通义 DashScope 文本向量嵌入，供 Milvus {@link org.springframework.ai.vectorstore.VectorStore} 使用。
 */
@Slf4j
public class DashScopeEmbeddingModel extends AbstractEmbeddingModel {

    private static final int BATCH_SIZE = 10;

    private final String apiKey;
    private final String model;
    private final int configuredDimensions;

    public DashScopeEmbeddingModel(String apiKey, String model, int configuredDimensions) {
        Assert.hasText(apiKey, "spring.ai.dashscope.api-key must not be empty");
        Assert.hasText(model, "embedding model must not be empty");
        this.apiKey = apiKey.trim();
        this.model = model.trim();
        this.configuredDimensions = configuredDimensions;
    }

    @Override
    public int dimensions() {
        if (configuredDimensions > 0) {
            return configuredDimensions;
        }
        return super.dimensions();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> texts = request.getInstructions();
        Assert.notNull(texts, "EmbeddingRequest instructions must not be null");
        List<org.springframework.ai.embedding.Embedding> out = new ArrayList<>();
        int globalIndex = 0;
        TextEmbedding client = new TextEmbedding();
        for (int start = 0; start < texts.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(start, end);
            TextEmbeddingParam param =
                    TextEmbeddingParam.builder()
                            .apiKey(apiKey)
                            .model(model)
                            .texts(batch)
                            .build();
            try {
                TextEmbeddingResult tr = client.call(param);
                if (tr == null || tr.getOutput() == null || tr.getOutput().getEmbeddings() == null) {
                    throw new IllegalStateException("DashScope embedding returned empty output");
                }
                List<TextEmbeddingResultItem> items = new ArrayList<>(tr.getOutput().getEmbeddings());
                items.sort(Comparator.comparing(i -> i.getTextIndex() != null ? i.getTextIndex() : 0));
                for (TextEmbeddingResultItem item : items) {
                    float[] vec = toFloatArray(item.getEmbedding());
                    if (configuredDimensions > 0 && vec.length != configuredDimensions) {
                        log.warn(
                                "embedding length {} differs from configured dimensions {}",
                                vec.length,
                                configuredDimensions);
                    }
                    out.add(new org.springframework.ai.embedding.Embedding(vec, globalIndex++));
                }
            } catch (Exception e) {
                log.warn("DashScope TextEmbedding failed: {}", e.toString());
                throw new RuntimeException("DashScope 文本向量调用失败: " + e.getMessage(), e);
            }
        }
        return new EmbeddingResponse(out);
    }

    @Override
    public float[] embed(Document document) {
        Assert.notNull(document, "Document must not be null");
        String text = document.getText();
        Assert.isTrue(StringUtils.hasText(text), "Document text must not be empty");
        return embed(text);
    }

    private static float[] toFloatArray(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalStateException("DashScope embedding vector is empty");
        }
        float[] out = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            out[i] = embedding.get(i).floatValue();
        }
        return out;
    }
}
