package com.neusoft.hospital.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "JWT 令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "过期时间戳(毫秒)", example = "1782216675000")
    private Long expiresAt;

    @Schema(description = "当前登录用户信息")
    private UserInfoResponse userInfo;
}
