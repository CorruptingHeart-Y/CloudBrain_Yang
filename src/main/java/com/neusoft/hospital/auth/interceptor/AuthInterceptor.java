package com.neusoft.hospital.auth.interceptor;

import com.neusoft.hospital.auth.annotation.Public;
import com.neusoft.hospital.auth.context.CurrentUser;
import com.neusoft.hospital.auth.jwt.JwtUtil;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (isPublic(handlerMethod)) {
            return true;
        }

        String token = extractToken(request);
        if (token == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Integer employeeId;
        try {
            employeeId = jwtUtil.parseEmployeeId(token);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        if (Boolean.TRUE.equals(redis.hasKey(BLACKLIST_KEY_PREFIX + token))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        CurrentUser.set(employeeId);
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

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Bearer");
        if (header != null ) {
            return header;
        }
        return null;
    }
}
