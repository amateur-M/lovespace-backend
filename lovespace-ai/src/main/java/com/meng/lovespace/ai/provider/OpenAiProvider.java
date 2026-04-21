package com.meng.lovespace.ai.provider;

import com.meng.lovespace.ai.dto.ChatTurn;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 * OpenAI 兼容接口，基于 Spring AI 自动配置的 {@link OpenAiChatModel}。
 *
 * <p>由 {@link com.meng.lovespace.ai.config.OpenAiProviderConfiguration} 在存在 {@link OpenAiChatModel} 时注册。
 */
public class OpenAiProvider implements LLMProvider {

    private final ChatClient chatClient;

    public OpenAiProvider(OpenAiChatModel openAiChatModel) {
        this.chatClient = ChatClient.builder(openAiChatModel).build();
    }

    @Override
    public String name() {
        return "openai";
    }

    @Override
    public String chat(String userMessage) {
        String content = chatClient.prompt().user(userMessage).call().content();
        return content != null ? content : "";
    }

    @Override
    public String chatWithSystem(String systemPrompt, String userContent) {
        String content =
                chatClient.prompt().system(systemPrompt).user(userContent).call().content();
        return content != null ? content : "";
    }

    @Override
    public String chatWithSystemAndHistory(
            String systemPrompt, List<ChatTurn> priorTurns, String currentUserMessage) {
        List<Message> springMessages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            springMessages.add(new SystemMessage(systemPrompt.trim()));
        }
        if (priorTurns != null) {
            for (ChatTurn t : priorTurns) {
                if (t == null) {
                    continue;
                }
                String body = t.content() != null ? t.content() : "";
                if ("user".equalsIgnoreCase(t.role())) {
                    springMessages.add(new UserMessage(body));
                } else {
                    springMessages.add(new AssistantMessage(body));
                }
            }
        }
        springMessages.add(new UserMessage(currentUserMessage));
        String content = chatClient.prompt(new Prompt(springMessages)).call().content();
        return content != null ? content : "";
    }

    @Override
    public void chatWithSystemAndHistoryStreaming(
            String systemPrompt,
            List<ChatTurn> priorTurns,
            String currentUserMessage,
            Consumer<String> onDelta) {
        List<Message> springMessages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            springMessages.add(new SystemMessage(systemPrompt.trim()));
        }
        if (priorTurns != null) {
            for (ChatTurn t : priorTurns) {
                if (t == null) {
                    continue;
                }
                String body = t.content() != null ? t.content() : "";
                if ("user".equalsIgnoreCase(t.role())) {
                    springMessages.add(new UserMessage(body));
                } else {
                    springMessages.add(new AssistantMessage(body));
                }
            }
        }
        springMessages.add(new UserMessage(currentUserMessage));
        Flux<String> flux = chatClient.prompt(new Prompt(springMessages)).stream().content();
        flux.doOnNext(chunk -> {
                    if (chunk != null && !chunk.isEmpty()) {
                        onDelta.accept(chunk);
                    }
                })
                .blockLast();
    }
}
