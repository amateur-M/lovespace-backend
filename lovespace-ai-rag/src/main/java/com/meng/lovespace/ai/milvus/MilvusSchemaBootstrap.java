package com.meng.lovespace.ai.milvus;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 应用启动后按需触发 Milvus 扩展集合的 ensure 骨架。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "lovespace.ai.rag", name = "enabled", havingValue = "true")
@ConditionalOnBean(MilvusSchemaService.class)
public class MilvusSchemaBootstrap {

    private final MilvusSchemaService milvusSchemaService;

    @PostConstruct
    void ensureOnStartup() {
        milvusSchemaService.ensureTravelPoiCollection();
    }
}
