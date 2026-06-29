package com.neusoft.hospital.auth.annotation;

import com.neusoft.hospital.auth.enums.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色授权注解。可标注在 Controller 方法或类上。
 * <p>
 * 当前登录用户的 role 不在 {@link #value()} 允许集合内时，拦截器返回真实 HTTP 403。
 * 角色之间平级、不做继承；必须显式列出允许角色。方法注解优先于类注解。
 * <p>
 * 未标注本注解的接口仅要求“已登录”，不做角色限制（留给后续 PR 逐接口细化）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    Role[] value();
}
