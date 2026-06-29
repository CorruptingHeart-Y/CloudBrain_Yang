package com.neusoft.hospital.auth;

import com.neusoft.hospital.auth.annotation.RequireRole;
import com.neusoft.hospital.auth.context.AuthUser;
import com.neusoft.hospital.auth.context.CurrentUser;
import com.neusoft.hospital.auth.enums.Role;
import com.neusoft.hospital.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【临时测试 Controller — 仅用于 PR2 端到端验证，不向生产接口增加调试入口】
 * 位于 src/test，不进入生产构建产物。
 */
@RestController
@RequestMapping("/api/v1/test")
public class TestRoleController {

    /** 验证 {@link RequireRole} 角色授权内核：仅 ADMIN 可访问。 */
    @RequireRole(Role.ADMIN)
    @GetMapping("/admin")
    public Result<String> adminOnly() {
        return Result.ok("admin-ok");
    }

    /** 任意已登录用户可访问（无 @RequireRole），用于探测 Controller 内能否读到正确 AuthUser。 */
    @GetMapping("/whoami")
    public Result<String> whoami() {
        AuthUser u = CurrentUser.getAuthUser();
        return Result.ok(u == null ? "ANONYMOUS" : u.getRole().name() + ":" + u.getAccountId());
    }

    /** @Public 探测端点：用于验证 @Public 请求开始时 ThreadLocal 是否被清空。 */
    @com.neusoft.hospital.auth.annotation.Public
    @GetMapping("/whoami-public")
    public Result<String> whoamiPublic() {
        AuthUser u = CurrentUser.getAuthUser();
        return Result.ok(u == null ? "ANONYMOUS" : u.getRole().name() + ":" + u.getAccountId());
    }
}
