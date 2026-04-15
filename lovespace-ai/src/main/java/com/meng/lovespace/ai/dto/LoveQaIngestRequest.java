package com.meng.lovespace.ai.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 恋爱知识库文档入库请求。
 *
 * @param text 原始文本
 * @param title 可选标题
 * @param metadata 可选附加元数据（如 source、tags）
 */
public record LoveQaIngestRequest(
        @NotBlank(message = "text 不能为空") String text, String title, Map<String, Object> metadata) {}
