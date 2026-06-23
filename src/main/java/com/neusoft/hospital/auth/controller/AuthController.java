package com.neusoft.hospital.auth.controller;

import com.neusoft.hospital.auth.dto.ChangePasswordRequest;
import com.neusoft.hospital.auth.dto.LoginRequest;
import com.neusoft.hospital.auth.dto.LoginResponse;
import com.neusoft.hospital.auth.dto.UserInfoResponse;
import com.neusoft.hospital.auth.service.AuthService;
import com.neusoft.hospital.common.Result;
import com.neusoft.hospital.auth.annotation.Public;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证", description = "登录/登出/当前用户/修改密码")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @Public
    @Operation(summary = "登录", description = "工号(id)+密码登录，返回 JWT。旧 MD5 密码会自动升级为 BCrypt")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @Operation(summary = "登出", description = "将当前 token 加入 Redis 黑名单，立即失效")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            authService.logout(token);
        }
        return Result.ok();
    }

    @Operation(summary = "当前登录用户信息", description = "返回当前 JWT 对应的员工基本信息(含科室、挂号级别名称)")
    @GetMapping("/me")
    public Result<UserInfoResponse> me() {
        return Result.ok(authService.currentUserInfo());
    }

    @Operation(summary = "修改密码", description = "校验原密码后改为新密码(BCrypt 加密)")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.ok();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
