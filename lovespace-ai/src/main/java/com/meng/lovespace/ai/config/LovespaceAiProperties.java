package com.meng.lovespace.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务侧 AI 路由：选择底层 {@link com.meng.lovespace.ai.provider.LLMProvider} 实现。
 */
@Data
@ConfigurationProperties(prefix = "lovespace.ai")
public class LovespaceAiProperties {

    /**
     * 使用的提供方：{@code qwen}（通义千问，DashScope）或 {@code openai}。
     */
    private String provider = "qwen";

    /** 情侣旅游规划：POI 向量检索等。 */
    private Travel travel = new Travel();

    @Data
    public static class Travel {
        /** 是否在规划前尝试用 Milvus 检索 POI 语义片段（需 POI 向量数据入库后才有意义）。 */
        private boolean poiVectorSearchEnabled = false;
    }
}
