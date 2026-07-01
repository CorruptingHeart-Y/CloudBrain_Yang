package com.neusoft.hospital.auth.context;

import com.neusoft.hospital.auth.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * 已认证身份（请求级，绑定在 {@link CurrentUser} 的 ThreadLocal 中）。
 * <p>
 * 来源只有一个：已签名校验通过的 JWT v2。绝不来自请求头或前端参数。
 */
@Getter
@Builder
@AllArgsConstructor
@ToString
public class AuthUser {

    /** user_account.id，JWT subject */
    private final Integer accountId;

    /** 平级角色，来自 user_account.role / JWT claim */
    private final Role role;

    /** 关联员工id，DOCTOR 必填，ADMIN 可空，PATIENT 为 null */
    private final Integer employeeId;

    /** 关联患者id，PATIENT 必填，其余为 null */
    private final Integer patientId;

    /** Token 版本号（来自 JWT claim tv / user_account.token_version），用于全局失效比对 */
    private final Integer tokenVersion;

    /** 真实姓名（展示用），可能为 null */
    private final String realname;
}
