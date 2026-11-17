package com.neusoft.cloudbrain.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器错误"),
    AI_UNAVAILABLE(503, "AI 服务不可用");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
