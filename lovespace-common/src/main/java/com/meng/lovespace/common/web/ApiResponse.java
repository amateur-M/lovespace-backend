package com.meng.lovespace.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一 API 响应封装。
 *
 * <p>约定：{@code code == 0} 表示成功，非 0 为业务错误码；{@code message} 为提示信息；{@code data} 为载荷（可为 null）。
 *
 * @param <T> 业务数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T data) {

    /**
     * 成功响应，携带数据。
     *
     * @param data 业务数据
     * @param <T> 数据类型
     * @return code=0 的响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    /**
     * 成功响应，无数据体。
     *
     * @return code=0、data 为 null 的响应
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null);
    }

    /**
     * 失败响应。
     *
     * @param code 业务错误码（非 0）
     * @param message 错误说明
     * @param <T> 泛型占位（data 恒为 null）
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}

