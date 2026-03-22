package com.meng.lovespace.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * 注册 {@link CommonsRequestLoggingFilter}，在 DEBUG 级别输出 HTTP 请求/响应摘要。
 */
@Configuration
public class RequestLoggingConfig {

    /**
     * @return 已配置前缀与包含项的请求日志过滤器
     */
    @Bean
    public CommonsRequestLoggingFilter commonsRequestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludeClientInfo(true);
        filter.setIncludeHeaders(false);
        filter.setIncludePayload(false);
        filter.setBeforeMessagePrefix("HTTP request: ");
        filter.setAfterMessagePrefix("HTTP response: ");
        return filter;
    }
}
