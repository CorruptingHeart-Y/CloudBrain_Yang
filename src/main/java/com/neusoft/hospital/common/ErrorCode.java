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
    BAD_CREDENTIALS(401, "工号或密码错误"),
    TOKEN_EXPIRED(401, "令牌已过期，请重新登录"),
    TOKEN_INVALID(401, "令牌无效"),
    ACCOUNT_LOCKED(423, "账号已锁定，请稍后再试"),
    PASSWORD_TOO_WEAK(400, "密码长度至少6位"),

    // AI 服务相关
    AI_UNAVAILABLE(503, "AI分诊服务暂不可用，请人工分诊"),
    AI_AUDIT_UNAVAILABLE(503, "AI处方审核服务暂不可用，请人工核对");

    private final Integer code;
    private final String message;
}
