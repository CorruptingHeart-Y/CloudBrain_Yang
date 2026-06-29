package com.neusoft.hospital.auth.context;

import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;

/**
 * 请求级当前登录用户上下文（ThreadLocal）。
 * <p>
 * PR2 升级：从只保存 {@code Integer employeeId} 升级为保存完整 {@link AuthUser}。
 * 保留旧的 {@link #getEmployeeId()} / {@link #get()} / {@link #require()} 兼容访问，
 * 新增 accountId / role / patientId 等读取方法。
 * <p>
 * 必须在请求结束时调用 {@link #clear()}，避免泄漏到线程复用的下一个请求。
 */
public final class CurrentUser {

    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(AuthUser user) {
        HOLDER.set(user);
    }

    /** 当前登录身份；未登录返回 null。 */
    public static AuthUser getAuthUser() {
        return HOLDER.get();
    }

    /** 要求已登录，否则抛 401 业务异常（真实 HTTP 401 由全局异常处理映射）。 */
    public static AuthUser requireAuthUser() {
        AuthUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    // ---------- 旧 API 兼容（保留现有调用方不破坏） ----------

    /**
     * 旧 API：返回当前登录用户的 employeeId（PATIENT / 无关联员工的 ADMIN 可能为 null）。
     */
    public static Integer get() {
        AuthUser user = HOLDER.get();
        return user == null ? null : user.getEmployeeId();
    }

    /** 旧 API：等价于 {@link #getEmployeeId()}。 */
    public static Integer getEmployeeId() {
        return get();
    }

    /**
     * 旧 API：要求已登录并返回 employeeId。
     * 注意：PATIENT 或无关联员工的 ADMIN 会得到 null，调用方需自行判空。
     * 如需强制已登录身份，请使用 {@link #requireAuthUser()}。
     */
    public static Integer require() {
        AuthUser user = requireAuthUser();
        return user.getEmployeeId();
    }

    // ---------- 新增读取方法 ----------

    public static Integer getAccountId() {
        AuthUser user = HOLDER.get();
        return user == null ? null : user.getAccountId();
    }

    public static Role getRole() {
        AuthUser user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static Integer getPatientId() {
        AuthUser user = HOLDER.get();
        return user == null ? null : user.getPatientId();
    }

    public static String getRealname() {
        AuthUser user = HOLDER.get();
        return user == null ? null : user.getRealname();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
