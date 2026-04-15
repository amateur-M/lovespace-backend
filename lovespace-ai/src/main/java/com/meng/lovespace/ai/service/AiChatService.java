package com.meng.lovespace.ai.service;

import com.meng.lovespace.ai.provider.LLMProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 按 {@code lovespace.ai.provider} 选择具体 {@link LLMProvider}。 */
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final LlmRouter llmRouter;

    /**
     * 执行一轮对话。
     *
     * @param userMessage 用户输入
     * @return 模型回复文本
     */
    public String chat(String userMessage) {
        LLMProvider provider = llmRouter.resolve();
        return provider.chat(userMessage);
    }
}
