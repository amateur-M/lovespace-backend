package com.meng.lovespace.user.config;

import com.meng.lovespace.ai.api.LoveQaChatFacade;
import com.meng.lovespace.user.controller.LoveQAController;
import com.meng.lovespace.user.service.LoveQaConversationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 恋爱知识库 RAG 接口仅在存在 {@link LoveQaChatFacade} 时注册；条件放在 {@link Configuration} 上，避免仅标在
 * {@link org.springframework.web.bind.annotation.RestController} 上时与组件扫描顺序冲突导致启动失败。
 */
@Configuration
@ConditionalOnBean(LoveQaChatFacade.class)
public class LoveQAControllerConfiguration {

    @Bean
    public LoveQAController loveQAController(
            LoveQaChatFacade loveQaChatFacade, LoveQaConversationService loveQaConversationService) {
        return new LoveQAController(loveQaChatFacade, loveQaConversationService);
    }
}
