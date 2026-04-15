package com.meng.lovespace.ai.rag.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 绑定 {@code lovespace.ai.rag.*} 与 {@code lovespace.milvus.*}（仅 lovespace-ai-rag 在 classpath 时生效）。 */
@Configuration
@EnableConfigurationProperties({RagAiProperties.class, MilvusProperties.class})
public class LoveSpaceRagPropertiesBindingConfiguration {}
