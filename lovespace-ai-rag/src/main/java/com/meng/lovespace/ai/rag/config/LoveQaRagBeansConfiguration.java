package com.meng.lovespace.ai.rag.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meng.lovespace.ai.milvus.MilvusClient;
import com.meng.lovespace.ai.milvus.MilvusSchemaService;
import com.meng.lovespace.ai.rag.DocumentIngestPipeline;
import com.meng.lovespace.ai.rag.LoveQAConversationStore;
import com.meng.lovespace.ai.rag.LoveQAService;
import com.meng.lovespace.ai.service.LlmRouter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 恋爱 RAG 相关 Bean 在此集中注册，条件放在 {@link Configuration} / {@link Bean} 方法上，避免仅标在
 * {@link org.springframework.stereotype.Service} 上时与组件扫描顺序冲突。
 */
@Configuration
@ConditionalOnProperty(prefix = "lovespace.ai.rag", name = "enabled", havingValue = "true")
public class LoveQaRagBeansConfiguration {

    @Bean
    @ConditionalOnBean(VectorStore.class)
    public LoveQAConversationStore loveQAConversationStore(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            RagAiProperties ragAiProperties) {
        return new LoveQAConversationStore(stringRedisTemplate, objectMapper, ragAiProperties);
    }

    @Bean
    @ConditionalOnBean(VectorStore.class)
    public LoveQAService loveQAService(
            VectorStore vectorStore,
            DocumentIngestPipeline documentIngestPipeline,
            LlmRouter llmRouter,
            RagAiProperties ragAiProperties,
            LoveQAConversationStore loveQAConversationStore) {
        return new LoveQAService(
                vectorStore, documentIngestPipeline, llmRouter, ragAiProperties, loveQAConversationStore);
    }

    @Bean
    @ConditionalOnBean(MilvusClient.class)
    public MilvusSchemaService milvusSchemaService(
            MilvusClient milvusClient, MilvusProperties milvusProperties) {
        return new MilvusSchemaService(milvusClient, milvusProperties);
    }
}
