package com.meng.lovespace.ai.service;

import com.meng.lovespace.ai.config.LovespaceAiProperties;
import com.meng.lovespace.ai.provider.LLMProvider;
import com.meng.lovespace.ai.provider.OpenAiProvider;
import com.meng.lovespace.ai.provider.QwenProvider;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 按 {@code lovespace.ai.provider} 解析 {@link LLMProvider}，与 {@link AiChatService} 行为一致。
 */
@Component
@RequiredArgsConstructor
public class LlmRouter {

    private final LovespaceAiProperties properties;
    private final ObjectProvider<QwenProvider> qwenProvider;
    private final ObjectProvider<OpenAiProvider> openAiProvider;

    public LLMProvider resolve() {
        String p = properties.getProvider() != null ? properties.getProvider().trim().toLowerCase(Locale.ROOT) : "qwen";
        if ("openai".equals(p)) {
            OpenAiProvider open = openAiProvider.getIfAvailable();
            if (open == null) {
                throw new IllegalStateException("未配置可用的 OpenAI：请设置 spring.ai.openai.api-key 并确保模型自动配置生效");
            }
            return open;
        }
        QwenProvider qwen = qwenProvider.getIfAvailable();
        if (qwen == null) {
            throw new IllegalStateException("未配置通义千问：请设置 spring.ai.dashscope.api-key");
        }
        return qwen;
    }
}
