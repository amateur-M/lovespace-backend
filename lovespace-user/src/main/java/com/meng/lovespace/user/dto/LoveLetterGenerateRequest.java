package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 情书生成请求。
 *
 * @param coupleId 情侣绑定 ID
 * @param style 风格：romantic / humorous / sincere
 * @param length 篇幅：short / medium / long（约 200 / 400 / 800 字）
 * @param memories 可选，用户补充的共同回忆；为空时由服务端从时间轴记录摘要拼接
 */
public record LoveLetterGenerateRequest(
        @NotBlank(message = "coupleId is required") String coupleId,
        @NotBlank(message = "style is required")
                @Pattern(
                        regexp = "romantic|humorous|sincere",
                        message = "style must be romantic, humorous or sincere")
                String style,
        @NotBlank(message = "length is required")
                @Pattern(
                        regexp = "short|medium|long",
                        message = "length must be short, medium or long")
                String length,
        @Size(max = 8000, message = "memories too long") String memories) {}
