package com.meng.lovespace.ai.milvus;

import com.meng.lovespace.ai.rag.config.MilvusProperties;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.index.DescribeIndexParam;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Milvus 集合管理：启动时按需确保恋爱知识库向量集合存在（与 Spring AI {@link MilvusVectorStore} 建表结构一致）；旅游
 * POI 第二集合见 {@link #ensureTravelPoiCollection()}。
 *
 * <p>由 {@link com.meng.lovespace.ai.rag.config.LoveQaRagBeansConfiguration} 注册为 Spring Bean。
 */
@Slf4j
@RequiredArgsConstructor
public class MilvusSchemaService {

    private static final int DEFAULT_EMBEDDING_DIM = 1536;
    private static final String DEFAULT_INDEX_PARAMS = "{\"nlist\":1024}";

    private final MilvusClient milvusClient;
    private final MilvusProperties milvusProperties;
    private final Environment environment;

    @PostConstruct
    void ensureOnStartup() {
        ensureLoveKnowledgeBaseCollection();
        ensureTravelPoiCollection();
    }

    /**
     * 与 Spring AI 1.0 {@link MilvusVectorStore#createCollection(String, String, String, boolean, String, String, String)}
     * 字段定义保持一致，确保入库前集合、索引与 load 状态就绪。
     */
    private void ensureLoveKnowledgeBaseCollection() {
        if (!milvusProperties.isEnsureLoveKnowledgeSchema()) {
            return;
        }
        MilvusServiceClient client = milvusClient.getServiceClient();
        String database =
                environment.getProperty(
                        "spring.ai.vectorstore.milvus.database-name", MilvusVectorStore.DEFAULT_DATABASE_NAME);
        String collection =
                environment.getProperty(
                        "spring.ai.vectorstore.milvus.collection-name", MilvusCollectionNames.LOVE_KNOWLEDGE_BASE);
        Integer dimObj = environment.getProperty("spring.ai.vectorstore.milvus.embedding-dimension", Integer.class);
        int embeddingDim = dimObj != null && dimObj > 0 ? dimObj : DEFAULT_EMBEDDING_DIM;

        String indexTypeStr = environment.getProperty("spring.ai.vectorstore.milvus.index-type", "IVF_FLAT");
        String metricStr = environment.getProperty("spring.ai.vectorstore.milvus.metric-type", "COSINE");
        IndexType indexType = IndexType.valueOf(indexTypeStr.trim());
        MetricType metricType = MetricType.valueOf(metricStr.trim());
        String indexParameters =
                environment.getProperty("spring.ai.vectorstore.milvus.index-parameters", DEFAULT_INDEX_PARAMS);

        boolean autoId =
                Boolean.parseBoolean(environment.getProperty("spring.ai.vectorstore.milvus.auto-id", "false"));
        String idFieldName =
                environment.getProperty(
                        "spring.ai.vectorstore.milvus.id-field-name", MilvusVectorStore.DOC_ID_FIELD_NAME);
        String contentFieldName =
                environment.getProperty(
                        "spring.ai.vectorstore.milvus.content-field-name", MilvusVectorStore.CONTENT_FIELD_NAME);
        String metadataFieldName =
                environment.getProperty(
                        "spring.ai.vectorstore.milvus.metadata-field-name", MilvusVectorStore.METADATA_FIELD_NAME);
        String embeddingFieldName =
                environment.getProperty(
                        "spring.ai.vectorstore.milvus.embedding-field-name", MilvusVectorStore.EMBEDDING_FIELD_NAME);

        R<Boolean> has =
                client.hasCollection(
                        HasCollectionParam.newBuilder()
                                .withDatabaseName(database)
                                .withCollectionName(collection)
                                .build());
        if (has.getException() != null) {
            throw new IllegalStateException("Milvus hasCollection failed: " + has.getException().getMessage(), has.getException());
        }
        if (Boolean.TRUE.equals(has.getData())) {
            log.debug("Milvus collection already exists: {}.{}", database, collection);
            ensureIndexAndLoaded(
                    client, database, collection, embeddingFieldName, indexType, metricType, indexParameters);
            return;
        }

        log.info("Creating Milvus collection {}.{} (dim={})", database, collection, embeddingDim);
        createLoveKnowledgeCollection(
                client,
                database,
                collection,
                idFieldName,
                autoId,
                contentFieldName,
                metadataFieldName,
                embeddingFieldName,
                embeddingDim);

        ensureIndexAndLoaded(
                client, database, collection, embeddingFieldName, indexType, metricType, indexParameters);
        log.info("Milvus collection ready: {}.{}", database, collection);
    }

    private static void createLoveKnowledgeCollection(
            MilvusServiceClient client,
            String databaseName,
            String collectionName,
            String idFieldName,
            boolean isAutoId,
            String contentFieldName,
            String metadataFieldName,
            String embeddingFieldName,
            int embeddingDimension) {
        FieldType docIdFieldType =
                FieldType.newBuilder()
                        .withName(idFieldName)
                        .withDataType(DataType.VarChar)
                        .withMaxLength(36)
                        .withPrimaryKey(true)
                        .withAutoID(isAutoId)
                        .build();
        FieldType contentFieldType =
                FieldType.newBuilder()
                        .withName(contentFieldName)
                        .withDataType(DataType.VarChar)
                        .withMaxLength(65535)
                        .build();
        FieldType metadataFieldType =
                FieldType.newBuilder()
                        .withName(metadataFieldName)
                        .withDataType(DataType.JSON)
                        .build();
        FieldType embeddingFieldType =
                FieldType.newBuilder()
                        .withName(embeddingFieldName)
                        .withDataType(DataType.FloatVector)
                        .withDimension(embeddingDimension)
                        .build();

        CreateCollectionParam createCollectionReq =
                CreateCollectionParam.newBuilder()
                        .withDatabaseName(databaseName)
                        .withCollectionName(collectionName)
                        .withDescription("Spring AI Vector Store (LoveSpace)")
                        .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                        .withShardsNum(2)
                        .addFieldType(docIdFieldType)
                        .addFieldType(contentFieldType)
                        .addFieldType(metadataFieldType)
                        .addFieldType(embeddingFieldType)
                        .build();

        R<?> collectionStatus = client.createCollection(createCollectionReq);
        if (collectionStatus.getException() != null) {
            throw new IllegalStateException(
                    "Failed to create Milvus collection " + databaseName + "." + collectionName,
                    collectionStatus.getException());
        }
    }

    private static void ensureIndexAndLoaded(
            MilvusServiceClient client,
            String databaseName,
            String collectionName,
            String embeddingFieldName,
            IndexType indexType,
            MetricType metricType,
            String indexParameters) {
        R<?> indexDescriptionResponse =
                client.describeIndex(
                        DescribeIndexParam.newBuilder()
                                .withDatabaseName(databaseName)
                                .withCollectionName(collectionName)
                                .build());

        if (indexDescriptionResponse.getData() == null) {
            String extra = StringUtils.hasText(indexParameters) ? indexParameters : DEFAULT_INDEX_PARAMS;
            R<?> indexStatus =
                    client.createIndex(
                            CreateIndexParam.newBuilder()
                                    .withDatabaseName(databaseName)
                                    .withCollectionName(collectionName)
                                    .withFieldName(embeddingFieldName)
                                    .withIndexType(indexType)
                                    .withMetricType(metricType)
                                    .withExtraParam(extra)
                                    .withSyncMode(Boolean.FALSE)
                                    .build());
            if (indexStatus.getException() != null) {
                throw new IllegalStateException("Failed to create Milvus index", indexStatus.getException());
            }
        }

        R<?> loadCollectionStatus =
                client.loadCollection(
                        LoadCollectionParam.newBuilder()
                                .withDatabaseName(databaseName)
                                .withCollectionName(collectionName)
                                .build());
        if (loadCollectionStatus.getException() != null) {
            throw new IllegalStateException("Milvus loadCollection failed", loadCollectionStatus.getException());
        }
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
