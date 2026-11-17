package com.neusoft.cloudbrain.common.exception;

import com.neusoft.cloudbrain.common.result.ResultCode;

public class AiServiceException extends BusinessException {

    public AiServiceException(String message) {
        super(ResultCode.AI_UNAVAILABLE, message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(ResultCode.AI_UNAVAILABLE, message);
        initCause(cause);
    }
}
