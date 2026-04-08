package com.meng.lovespace.ai.service;

import com.meng.lovespace.ai.config.LovespaceAiProperties;
import com.meng.lovespace.ai.provider.LLMProvider;
import com.meng.lovespace.ai.provider.OpenAiProvider;
import com.meng.lovespace.ai.provider.QwenProvider;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** 按 {@code lovespace.ai.provider} 选择具体 {@link LLMProvider}。 */
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final LovespaceAiProperties properties;
    private final ObjectProvider<QwenProvider> qwenProvider;
    private final ObjectProvider<OpenAiProvider> openAiProvider;

    /**
     * 执行一轮对话。
     *
     * @param userMessage 用户输入
     * @return 模型回复文本
     */
    public String chat(String userMessage) {
        LLMProvider provider = resolveProvider();
        return provider.chat(userMessage);
    }

    private LLMProvider resolveProvider() {
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
