package com.meng.lovespace.ai.provider;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
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
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("spring.ai.dashscope.api-key 未配置");
        }
        Message userMsg = Message.builder().role(Role.USER.getValue()).content(userMessage).build();
        GenerationParam param =
                GenerationParam.builder()
                        .apiKey(apiKey.trim())
                        .model(model)
                        .messages(List.of(userMsg))
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
