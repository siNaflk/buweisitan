package com.xunye.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码枚举
 * <p>分段规则：200 成功；400/401/403/500 通用；5001~5999 业务错误
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "成功"),

    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),
    SYS_ERROR(500, "系统内部错误"),

    BALANCE_INSUFFICIENT(5001, "余额不足"),
    REPEAT_BUY(5002, "请勿重复购买"),
    VIP_INVALID(5003, "会员未生效或已过期"),
    CONTENT_NOT_ONLINE(5004, "内容未过审或已下架"),
    USER_BANNED(5005, "账号已被封禁"),
    BOOK_NOT_EXIST(5006, "作品不存在或已下架"),
    CHAPTER_NOT_ONLINE(5007, "章节不存在或未过审");

    private final int code;
    private final String msg;
}
