package com.meng.lovespace.ai.rag.env;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 当 {@code lovespace.ai.rag.enabled} 不为 true 时，排除 Spring AI 的 Milvus 向量库自动配置，避免在未部署 Milvus 时仍尝试连接。
 */
public class RagMilvusAutoConfigurationExclusionEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String EXCLUDE_KEY = "spring.autoconfigure.exclude";

    private static final String MILVUS_AUTO_CONFIG =
            "org.springframework.ai.autoconfigure.vectorstore.milvus.MilvusVectorStoreAutoConfiguration";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (isRagEnabled(environment)) {
            return;
        }
        String current = environment.getProperty(EXCLUDE_KEY);
        if (current != null && current.contains(MILVUS_AUTO_CONFIG)) {
            return;
        }
        String merged;
        if (current == null || current.isBlank()) {
            merged = MILVUS_AUTO_CONFIG;
        } else {
            merged = current + "," + MILVUS_AUTO_CONFIG;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(EXCLUDE_KEY, merged);
        environment.getPropertySources().addFirst(new MapPropertySource("lovespaceRagMilvusExclude", map));
    }

    private static boolean isRagEnabled(ConfigurableEnvironment environment) {
        Boolean b = environment.getProperty("lovespace.ai.rag.enabled", Boolean.class);
        if (b != null) {
            return b;
        }
        String raw = environment.getProperty("lovespace.ai.rag.enabled");
        return raw != null && "true".equalsIgnoreCase(raw.trim());
    }
}
