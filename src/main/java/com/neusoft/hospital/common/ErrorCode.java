package com.neusoft.hospital.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    BUSINESS_ERROR(500, "业务异常"),
    SYSTEM_ERROR(500, "系统内部错误"),

    // 鉴权相关
    UNAUTHORIZED(401, "未登录"),
    BAD_CREDENTIALS(401, "账号或密码错误"),
    TOKEN_EXPIRED(401, "令牌已过期，请重新登录"),
    TOKEN_INVALID(401, "令牌无效"),
    ACCOUNT_DISABLED(401, "账号已停用或不存在"),
    ACCOUNT_LOCKED(423, "账号已锁定，请稍后再试"),
    FORBIDDEN(403, "权限不足"),
    PASSWORD_TOO_WEAK(400, "密码长度至少6位");

    private final Integer code;
    private final String message;
}
