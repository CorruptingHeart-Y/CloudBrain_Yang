package com.neusoft.hospital.auth.context;

public final class CurrentUser {

    private static final ThreadLocal<Integer> HOLDER = new ThreadLocal<>();

    private CurrentUser() {}

    public static void set(Integer employeeId) {
        HOLDER.set(employeeId);
    }

    public static Integer get() {
        return HOLDER.get();
    }

    public static Integer require() {
        Integer id = HOLDER.get();
        if (id == null) {
            throw new IllegalStateException("当前线程未绑定登录用户，请检查拦截器配置");
        }
        return id;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
