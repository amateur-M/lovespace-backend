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

    /**
     * 系统提示词 + 用户内容（RAG、结构化 JSON 输出等）。
     *
     * @param systemPrompt 系统侧指令或检索上下文说明
     * @param userContent 用户问题或待处理文本
     * @return 模型回复正文
     */
    default String chatWithSystem(String systemPrompt, String userContent) {
        throw new UnsupportedOperationException("chatWithSystem not supported for provider: " + name());
    }
}
