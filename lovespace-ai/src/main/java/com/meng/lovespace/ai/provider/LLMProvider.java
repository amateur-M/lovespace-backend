package com.meng.lovespace.ai.provider;

/**
 * 大模型调用抽象，便于在通义千问与 OpenAI 等实现之间切换。
 */
public interface LLMProvider {

    /**
     * @return 实现标识，例如 {@code qwen}、{@code openai}
     */
    String name();

    /**
     * 单轮对话：仅发送用户消息，返回模型文本回复。
     *
     * @param userMessage 用户输入（非空由上层校验）
     * @return 模型回复正文
     */
    String chat(String userMessage);
}
