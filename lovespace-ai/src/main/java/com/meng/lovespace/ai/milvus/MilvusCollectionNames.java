package com.meng.lovespace.ai.milvus;

/**
 * Milvus Collection 名称约定（与配置 {@code spring.ai.vectorstore.milvus.collection-name} 及业务 POI 集合一致）。
 */
public final class MilvusCollectionNames {

    /** 恋爱知识库 RAG 向量集合。 */
    public static final String LOVE_KNOWLEDGE_BASE = "love_knowledge_base";

    /** 旅游 POI（景点/餐厅等）语义检索向量集合（可选）。 */
    public static final String TRAVEL_POI_EMBEDDINGS = "travel_poi_embeddings";

    private MilvusCollectionNames() {}
}
