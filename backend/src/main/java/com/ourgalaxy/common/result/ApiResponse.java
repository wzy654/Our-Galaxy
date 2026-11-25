package com.ourgalaxy.common.result;

import java.time.Instant;

public record ApiResponse<T>(int code, String message, T data, Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, Instant.now());
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(0, "success", null, Instant.now());
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now());
    }
}
