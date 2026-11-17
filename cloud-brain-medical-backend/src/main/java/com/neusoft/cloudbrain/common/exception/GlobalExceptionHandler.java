package com.neusoft.cloudbrain.common.exception;

import com.neusoft.cloudbrain.common.result.Result;
import com.neusoft.cloudbrain.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException ex) {
        return Result.fail(ex.getResultCode(), ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return Result.fail(ResultCode.PARAM_ERROR, "请求体格式错误，请选择 raw / JSON(application/json) 提交");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleValidationException(Exception ex) {
        String message = ResultCode.PARAM_ERROR.getMessage();
        if (ex instanceof MethodArgumentNotValidException validException
                && validException.getBindingResult().getFieldError() != null) {
            message = validException.getBindingResult().getFieldError().getDefaultMessage();
        }
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleAccessDeniedException(AccessDeniedException ex) {
        return Result.fail(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.fail(ResultCode.SERVER_ERROR);
    }
}
