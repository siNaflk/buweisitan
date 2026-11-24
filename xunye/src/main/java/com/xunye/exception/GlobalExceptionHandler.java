package com.xunye.exception;

import com.xunye.common.Result;
import com.xunye.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：兜底所有 Controller 抛出的异常，统一转换为 Result
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按错误码返回 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验异常：返回第一个校验失败信息 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? ErrorCode.PARAM_ERROR.getMsg() : fieldError.getDefaultMessage();
        return Result.fail(ErrorCode.PARAM_ERROR, msg);
    }

    /** URL参数类型错误、缺少必填参数、JSON无法解析属于客户端输入错误，统一返回400业务码。 */
    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public Result<Void> handleBadInput(Exception e) { return Result.fail(ErrorCode.PARAM_ERROR); }

    /** 未知异常：记录堆栈，对外只提示系统错误 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.SYS_ERROR);
    }
}
