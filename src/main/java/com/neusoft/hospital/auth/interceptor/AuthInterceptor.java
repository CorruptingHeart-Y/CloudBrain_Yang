package com.neusoft.hospital.auth.interceptor;

import com.neusoft.hospital.auth.annotation.Public;
import com.neusoft.hospital.auth.annotation.RequireRole;
import com.neusoft.hospital.auth.context.AuthUser;
import com.neusoft.hospital.auth.context.CurrentUser;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.auth.jwt.JwtUtil;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.entity.UserAccount;
import com.neusoft.hospital.mapper.UserAccountMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final JwtUtil jwtUtil;
    private final UserAccountMapper userAccountMapper;
    private final StringRedisTemplate redis;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 防御性清理：每个请求开始先清空 ThreadLocal，杜绝同一线程上残留旧身份污染新请求
        // （即使未来某条异常路径遗漏 afterCompletion，也不会泄漏；@Public 也从干净上下文开始）
        CurrentUser.clear();

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (isPublic(handlerMethod)) {
            return true;
        }

        // 1. Token 存在性
        String token = extractToken(request);
        if (token == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 2. 解析并校验 JWT v2（签名 / 过期 / ver=2 / role 合法）
        AuthUser jwtUser;
        try {
            jwtUser = jwtUtil.parse(token);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            // 旧 employee-only Token（无 ver）、伪造 Token、签名错误统一 401
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 3. 黑名单（登出）校验
        if (Boolean.TRUE.equals(redis.hasKey(BLACKLIST_KEY_PREFIX + token))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 4. 校验 user_account 仍启用、未逻辑删除；角色以数据库为准（绝不信任请求头/前端）
        UserAccount account = userAccountMapper.selectById(jwtUser.getAccountId());
        if (account == null || account.getStatus() == null || account.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        Role currentRole = Role.fromString(account.getRole());
        if (currentRole == null) {
            log.error("user_account.id={} 的 role 值非法: {}", account.getId(), account.getRole());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        AuthUser authUser = AuthUser.builder()
                .accountId(account.getId())
                .role(currentRole)
                .employeeId(account.getEmployeeId())
                .patientId(account.getPatientId())
                .realname(jwtUser.getRealname())
                .build();

        // 5. 角色授权：解析方法/类上的 @RequireRole，当前 role 不在允许集合 → 403
        //    注意：set() 故意放在 @RequireRole 之后——任何 401/403/禁用/解析异常路径
        //    都不会在 ThreadLocal 中留下 AuthUser，避免 afterCompletion 被跳过时泄漏。
        RequireRole requireRole = resolveRequireRole(handlerMethod);
        if (requireRole != null) {
            Role[] allowed = requireRole.value();
            boolean ok = allowed != null && allowed.length > 0
                    && Arrays.asList(allowed).contains(currentRole);
            if (!ok) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        } else {
            // PR3：PATIENT 默认拒绝未显式声明 @RequireRole 的旧业务通用接口。
            //   - 旧 Controller（register/prescription/check-request/inspection-request/
            //     disposal-request 等）均无 @RequireRole，对 PATIENT 一律 403，先堵越权；
            //   - ADMIN / DOCTOR 维持 PR2 当前行为（仅要求已登录），完整矩阵留待 PR4；
            //   - PATIENT 仅能访问显式 @RequireRole 包含 PATIENT 的接口（如 PatientController、
            //     AuthController 登录后接口）。
            //   注意：仍处于 set() 之前，403 路径不在 ThreadLocal 残留 AuthUser。
            if (currentRole == Role.PATIENT) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }

        CurrentUser.set(authUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUser.clear();
    }

    private boolean isPublic(HandlerMethod handlerMethod) {
        if (handlerMethod.hasMethodAnnotation(Public.class)) {
            return true;
        }
        Class<?> beanType = handlerMethod.getBeanType();
        return beanType.isAnnotationPresent(Public.class);
    }

    /** 方法级 @RequireRole 优先于类级。 */
    private RequireRole resolveRequireRole(HandlerMethod handlerMethod) {
        RequireRole onMethod = handlerMethod.getMethod().getAnnotation(RequireRole.class);
        if (onMethod != null) {
            return onMethod;
        }
        return handlerMethod.getBeanType().getAnnotation(RequireRole.class);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
