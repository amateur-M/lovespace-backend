package com.meng.lovespace.ai.rag.config;

import com.meng.lovespace.ai.milvus.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 Milvus 原生客户端封装；依赖 Spring AI 自动配置的 {@link MilvusVectorStore}。
 */
@Configuration
@ConditionalOnProperty(prefix = "lovespace.ai.rag", name = "enabled", havingValue = "true")
public class RagMilvusConfiguration {

    @Bean
    @ConditionalOnBean(MilvusVectorStore.class)
    public MilvusClient milvusClient(MilvusVectorStore milvusVectorStore) {
        MilvusServiceClient nativeClient =
                milvusVectorStore
                        .getNativeClient()
                        .map(MilvusServiceClient.class::cast)
                        .orElseThrow(() -> new IllegalStateException("Milvus native client unavailable"));
        return new MilvusClient(nativeClient);
    }
}
