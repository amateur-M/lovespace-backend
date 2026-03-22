package com.meng.lovespace.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口，供负载均衡或运维探活使用。
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * 返回简单文本表示服务可用。
     *
     * @return 固定字符串 {@code ok}
     */
    @Operation(summary = "Health check")
    @GetMapping
    public String health() {
        return "ok";
    }
}

