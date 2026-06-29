package com.neusoft.hospital.auth.enums;

/**
 * 平级角色枚举（不做角色继承）。
 * <p>
 * ADMIN / DOCTOR / PATIENT 三者平级，后续 Controller 必须显式列出允许的角色，
 * 不得互相包含。后端只信任数据库中的 user_account.role 与已签名 JWT 中的 role，
 * 绝不信任前端 localStorage、请求参数或 X-Role 等请求头。
 */
public enum Role {
    ADMIN,
    DOCTOR,
    PATIENT;

    /**
     * 大小写宽容地解析角色字符串；非法值返回 null（调用方据此判定 Token 无效）。
     */
    public static Role fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
