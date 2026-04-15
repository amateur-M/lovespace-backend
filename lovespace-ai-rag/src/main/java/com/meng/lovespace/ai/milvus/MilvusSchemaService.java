package com.meng.lovespace.ai.milvus;

import com.meng.lovespace.ai.rag.config.MilvusProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Milvus 集合与索引管理骨架：RAG 主集合一般由 Spring AI MilvusVectorStore 的 {@code initialize-schema} 维护；
 * 第二集合 {@link MilvusCollectionNames#TRAVEL_POI_EMBEDDINGS} 的创建与索引在此预留扩展点。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "lovespace.ai.rag", name = "enabled", havingValue = "true")
@ConditionalOnBean(MilvusClient.class)
public class MilvusSchemaService {

    private final MilvusClient milvusClient;
    private final MilvusProperties milvusProperties;

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
