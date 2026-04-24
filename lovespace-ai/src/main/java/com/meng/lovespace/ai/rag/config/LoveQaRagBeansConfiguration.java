package com.meng.lovespace.ai.rag.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meng.lovespace.ai.milvus.MilvusClient;
import com.meng.lovespace.ai.milvus.MilvusSchemaService;
import com.meng.lovespace.ai.rag.DocumentIngestPipeline;
import com.meng.lovespace.ai.rag.LoveQAConversationStore;
import com.meng.lovespace.ai.rag.LoveQAService;
import com.meng.lovespace.ai.service.LlmRouter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

/** 恋爱 RAG 相关 Bean 在此集中注册。 */
@Configuration
public class LoveQaRagBeansConfiguration {

    @Bean
    public LoveQAConversationStore loveQAConversationStore(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            RagAiProperties ragAiProperties) {
        return new LoveQAConversationStore(stringRedisTemplate, objectMapper, ragAiProperties);
    }

    @Bean
    @DependsOn("milvusSchemaService")
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
    public MilvusSchemaService milvusSchemaService(
            MilvusClient milvusClient, MilvusProperties milvusProperties, Environment environment) {
        return new MilvusSchemaService(milvusClient, milvusProperties, environment);
    }
}
