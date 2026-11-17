package com.neusoft.cloudbrain.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public static <T> Result<T> success(T data) {
        return of(ResultCode.SUCCESS, data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return of(resultCode, null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        Result<T> result = of(resultCode, null);
        result.setMessage(message);
        return result;
    }

    private static <T> Result<T> of(ResultCode resultCode, T data) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), data, System.currentTimeMillis());
    }
}
