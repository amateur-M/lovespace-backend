package com.meng.lovespace.ai.milvus;

import io.milvus.client.MilvusServiceClient;
import lombok.Getter;

/**
 * Milvus 原生客户端的单例封装：由 {@link com.meng.lovespace.ai.rag.config.RagMilvusConfiguration} 注册为 Bean，
 * 底层实例来自 Spring AI {@link org.springframework.ai.vectorstore.milvus.MilvusVectorStore#getNativeClient()}，与向量库共用连接。
 */
public final class MilvusClient {

    @Getter
    private final MilvusServiceClient serviceClient;

    public MilvusClient(MilvusServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }
}
