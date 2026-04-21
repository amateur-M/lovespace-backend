package com.meng.lovespace.ai.rag.config;

import com.meng.lovespace.ai.milvus.MilvusCollectionNames;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LoveSpace 侧 Milvus 扩展配置（第二集合、建表开关等）。连接参数优先使用 {@code spring.ai.vectorstore.milvus.*}。
 */
@Data
@ConfigurationProperties(prefix = "lovespace.milvus")
public class MilvusProperties {

    /**
     * 启动时若 {@code spring.ai.vectorstore.milvus.collection-name} 所指集合尚不存在，则按 Spring AI
     * {@code MilvusVectorStore} 同款结构建表、建索引并 load（避免仅靠 {@code initialize-schema} 时仍出现集合未就绪）。
     */
    private boolean ensureLoveKnowledgeSchema = true;

    /**
     * 是否在启动时尝试为 {@link MilvusCollectionNames#TRAVEL_POI_EMBEDDINGS} 执行建表/建索引骨架逻辑。
     */
    private boolean ensureTravelPoiSchema = false;

    /** 旅游 POI 集合名（默认与常量一致，可覆盖）。 */
    private String travelPoiCollectionName = MilvusCollectionNames.TRAVEL_POI_EMBEDDINGS;
}
