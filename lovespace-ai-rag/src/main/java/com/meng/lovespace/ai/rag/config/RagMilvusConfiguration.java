package com.meng.lovespace.ai.rag.config;

import com.meng.lovespace.ai.milvus.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册业务侧 Milvus 客户端封装。Bean 名不可为 {@code milvusClient}，以免与 Spring AI
 * {@code MilvusVectorStoreAutoConfiguration} 中同名 Bean 冲突。
 */
@Configuration
public class RagMilvusConfiguration {

    /** 与 Spring AI 自动配置中的 {@code milvusClient} 区分，仅本模块按类型注入 {@link MilvusClient}。 */
    @Bean(name = "lovespaceMilvusClient")
    public MilvusClient lovespaceMilvusClient(MilvusVectorStore milvusVectorStore) {
        MilvusServiceClient nativeClient =
                milvusVectorStore
                        .getNativeClient()
                        .map(MilvusServiceClient.class::cast)
                        .orElseThrow(() -> new IllegalStateException("Milvus native client unavailable"));
        return new MilvusClient(nativeClient);
    }
}
