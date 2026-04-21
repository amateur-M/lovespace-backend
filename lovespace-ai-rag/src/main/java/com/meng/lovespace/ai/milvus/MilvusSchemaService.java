package com.meng.lovespace.ai.milvus;

import com.meng.lovespace.ai.rag.config.MilvusProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * Milvus 集合与索引管理骨架：RAG 主集合一般由 Spring AI MilvusVectorStore 的 {@code initialize-schema} 维护；
 * 第二集合 {@link MilvusCollectionNames#TRAVEL_POI_EMBEDDINGS} 的创建与索引在此预留扩展点。
 *
 * <p>由 {@link com.meng.lovespace.ai.rag.config.LoveQaRagBeansConfiguration} 在存在 {@link MilvusClient} 时注册。
 */
@Slf4j
@RequiredArgsConstructor
public class MilvusSchemaService {

    private final MilvusClient milvusClient;
    private final MilvusProperties milvusProperties;

    @PostConstruct
    void ensureOnStartup() {
        ensureTravelPoiCollection();
    }

    /**
     * 确保旅游 POI 向量集合存在（骨架：当前仅记录日志，后续可在此调用 SDK 执行 HasCollection / CreateCollection / createIndex）。
     */
    public void ensureTravelPoiCollection() {
        if (!milvusProperties.isEnsureTravelPoiSchema()) {
            return;
        }
        String name = milvusProperties.getTravelPoiCollectionName();
        log.info(
                "MilvusSchemaService.ensureTravelPoiCollection: skeleton for collection={}, clientPresent={}",
                name,
                milvusClient.getServiceClient() != null);
    }
}
