package com.meng.lovespace.ai.provider;

import com.meng.lovespace.ai.dto.ChatTurn;
import java.util.List;
import java.util.function.Consumer;

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

    /**
     * 系统提示（如 RAG 指令与检索片段）+ 已发生对话轮次 + 本轮用户问题。
     *
     * <p>默认实现将历史拼进 system 再调用 {@link #chatWithSystem}；通义/OpenAI 等实现宜覆盖为原生多轮
     * messages，避免模型忽略埋在超长 system 里的「上轮对话」。
     *
     * @param systemPrompt 系统侧指令与上下文；可为空（仅多轮 + 本轮）
     * @param priorTurns 按时间顺序、{@code user}/{@code assistant} 交替；不含本轮用户输入
     * @param currentUserMessage 本轮用户问题
     */
    default String chatWithSystemAndHistory(
            String systemPrompt, List<ChatTurn> priorTurns, String currentUserMessage) {
        if (priorTurns == null || priorTurns.isEmpty()) {
            return chatWithSystem(systemPrompt, currentUserMessage);
        }
        StringBuilder sb = new StringBuilder(systemPrompt != null ? systemPrompt : "");
        sb.append("\n\n【本轮之前的对话】\n");
        for (ChatTurn t : priorTurns) {
            String label = t != null && "user".equalsIgnoreCase(t.role()) ? "用户" : "助手";
            String text = t != null && t.content() != null ? t.content() : "";
            sb.append(label).append(": ").append(text).append("\n");
        }
        return chatWithSystem(sb.toString(), currentUserMessage);
    }

    /**
     * 与 {@link #chatWithSystemAndHistory} 等价语义，但以增量形式回调 {@code onDelta}。
     *
     * <p>默认实现：一次性回调整段回复。
     */
    default void chatWithSystemAndHistoryStreaming(
            String systemPrompt,
            List<ChatTurn> priorTurns,
            String currentUserMessage,
            Consumer<String> onDelta) {
        String full = chatWithSystemAndHistory(systemPrompt, priorTurns, currentUserMessage);
        if (full != null && !full.isEmpty()) {
            onDelta.accept(full);
        }
    }
}
