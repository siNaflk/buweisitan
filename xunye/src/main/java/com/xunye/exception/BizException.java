package com.xunye.exception;

import com.xunye.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常：service 层遇到业务规则不满足时抛出，
 * 由 GlobalExceptionHandler 统一转换为 Result 返回
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String msg) {
        super(msg);
        this.code = errorCode.getCode();
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
