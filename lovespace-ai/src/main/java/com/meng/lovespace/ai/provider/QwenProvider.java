package com.meng.lovespace.ai.provider;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.meng.lovespace.ai.dto.ChatTurn;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 通义千问（阿里云 DashScope），使用官方 dashscope-sdk-java。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.ai.dashscope", name = "api-key")
public class QwenProvider implements LLMProvider {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    /** 默认模型名，与 {@code spring.ai.model} 对齐 */
    @Value("${spring.ai.model:qwen-turbo}")
    private String model;

    @Override
    public String name() {
        return "qwen";
    }

    @Override
    public String chat(String userMessage) {
        Message userMsg = Message.builder().role(Role.USER.getValue()).content(userMessage).build();
        return complete(List.of(userMsg));
    }

    /**
     * 使用系统提示词 + 用户内容调用通义千问（适用于结构化输出、情感分析等场景）。
     *
     * @param systemPrompt 系统角色说明
     * @param userContent 用户侧数据或指令
     * @return 模型完整文本
     */
    @Override
    public String chatWithSystem(String systemPrompt, String userContent) {
        List<Message> messages =
                List.of(
                        Message.builder().role(Role.SYSTEM.getValue()).content(systemPrompt).build(),
                        Message.builder().role(Role.USER.getValue()).content(userContent).build());
        return complete(messages);
    }

    /**
     * 使用 DashScope 原生多轮 messages，避免把整段对话塞进单条 system 导致模型不遵循上文。
     */
    @Override
    public String chatWithSystemAndHistory(
            String systemPrompt, List<ChatTurn> priorTurns, String currentUserMessage) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(Message.builder().role(Role.SYSTEM.getValue()).content(systemPrompt.trim()).build());
        }
        if (priorTurns != null) {
            for (ChatTurn t : priorTurns) {
                if (t == null) {
                    continue;
                }
                String body = t.content() != null ? t.content() : "";
                if ("user".equalsIgnoreCase(t.role())) {
                    messages.add(Message.builder().role(Role.USER.getValue()).content(body).build());
                } else {
                    messages.add(Message.builder().role(Role.ASSISTANT.getValue()).content(body).build());
                }
            }
        }
        messages.add(Message.builder().role(Role.USER.getValue()).content(currentUserMessage).build());
        return complete(messages);
    }

    private String complete(List<Message> messages) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("spring.ai.dashscope.api-key 未配置");
        }
        GenerationParam param =
                GenerationParam.builder()
                        .apiKey(apiKey.trim())
                        .model(model)
                        .messages(messages)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .build();
        try {
            Generation gen = new Generation();
            GenerationResult result = gen.call(param);
            if (result == null
                    || result.getOutput() == null
                    || result.getOutput().getChoices() == null
                    || result.getOutput().getChoices().isEmpty()) {
                throw new IllegalStateException("DashScope 返回为空");
            }
            String text = result.getOutput().getChoices().get(0).getMessage().getContent();
            return text != null ? text : "";
        } catch (Exception e) {
            log.warn("DashScope 调用失败: {}", e.toString());
            throw new RuntimeException("通义千问调用失败: " + e.getMessage(), e);
        }
    }
}
