package com.meng.lovespace.ai.config;

import com.meng.lovespace.ai.travel.AmapPlacesClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** AI 模块内无自动配置的 Bean（如高德占位客户端）。 */
@Configuration
public class AiInfraBeansConfiguration {

    @Bean
    public AmapPlacesClient amapPlacesClient() {
        return new AmapPlacesClient.NoOp();
    }
}
