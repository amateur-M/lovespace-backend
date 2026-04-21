package com.meng.lovespace.ai.config;

import com.meng.lovespace.ai.embedding.DashScopeEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 在选用 DashScope 文本嵌入时注册 {@link EmbeddingModel}，与通义千问共用 {@code spring.ai.dashscope.api-key}（Milvus 向量入库与检索依赖）。
 */
@Configuration
public class DashScopeEmbeddingConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    @ConditionalOnProperty(prefix = "lovespace.ai.embedding", name = "provider", havingValue = "dashscope", matchIfMissing = true)
    public EmbeddingModel dashScopeEmbeddingModel(
            LovespaceAiProperties lovespaceAiProperties,
            @Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("使用 DashScope 文本嵌入时，请配置 spring.ai.dashscope.api-key");
        }
        LovespaceAiProperties.Embedding e = lovespaceAiProperties.getEmbedding();
        return new DashScopeEmbeddingModel(apiKey.trim(), e.getModel(), e.getDimensions());
    }
}
