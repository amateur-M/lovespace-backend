package com.meng.lovespace.ai.config;

import com.meng.lovespace.ai.provider.OpenAiProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link OpenAiProvider} 在此注册，条件放在 {@link Configuration} 上，避免仅标在
 * {@link org.springframework.stereotype.Component} 上时与组件扫描顺序冲突。
 */
@Configuration
public class OpenAiProviderConfiguration {

    @Bean
    @ConditionalOnBean(OpenAiChatModel.class)
    public OpenAiProvider openAiProvider(OpenAiChatModel openAiChatModel) {
        return new OpenAiProvider(openAiChatModel);
    }
}
