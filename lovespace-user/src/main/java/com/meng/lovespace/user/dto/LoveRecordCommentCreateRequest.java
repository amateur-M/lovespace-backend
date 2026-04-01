package com.meng.lovespace.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 恋爱记录评论创建请求体。 */
public record LoveRecordCommentCreateRequest(
        @NotBlank(message = "content is required") @Size(max = 500, message = "content too long") String content) {}
