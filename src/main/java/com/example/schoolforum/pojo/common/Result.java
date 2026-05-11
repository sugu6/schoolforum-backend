package com.example.schoolforum.pojo.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class Result<T> {

    private Integer code;
    private String msg;
    private T data;
    private Long timestamp;

    public static <T> Result<T> success() {
        return Result.<T>builder()
                .code(200)
                .msg("success")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .msg("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> success(T data, String msg) {
        return Result.<T>builder()
                .code(200)
                .msg(msg)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> success(String msg) {
        return Result.<T>builder()
                .code(200)
                .msg(msg)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> error(String msg) {
        return Result.<T>builder()
                .code(500)
                .msg(msg)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return Result.<T>builder()
                .code(code)
                .msg(msg)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> Result<T> error(Integer code, String msg, T data) {
        return Result.<T>builder()
                .code(code)
                .msg(msg)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
