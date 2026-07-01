package com.neusoft.hospital.auth.jwt;

import com.neusoft.hospital.auth.context.AuthUser;
import com.neusoft.hospital.auth.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    /** JWT claims 版本号，v2 = 账号化身份（sub=accountId，含 role 等）。 */
    public static final int VER = 2;
    public static final String CLAIM_VER = "ver";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_EMPLOYEE_ID = "employeeId";
    public static final String CLAIM_PATIENT_ID = "patientId";
    public static final String CLAIM_REALNAME = "realname";
    /** PR5：Token 版本号，与 user_account.token_version 比对，递增后历史 Token 失效 */
    public static final String CLAIM_TOKEN_VERSION = "tv";

    @Value("${hospital.jwt.secret}")
    private String secret;

    @Value("${hospital.jwt.expire-hours:8}")
    private long expireHours;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] bytes = looksLikeBase64(secret) ? Decoders.BASE64.decode(secret) : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("hospital.jwt.secret 长度不足，HS256 要求至少 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * 签发 JWT v2。sub = accountId，claims 含 ver/role/employeeId/patientId/realname/tv。
     * 不放入密码、身份证号、地址、手机号等敏感信息。
     */
    public String generate(AuthUser user) {
        long now = System.currentTimeMillis();
        long exp = expireHours * 3600_000L;
        var builder = Jwts.builder()
                .subject(String.valueOf(user.getAccountId()))
                .claim(CLAIM_VER, VER)
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion() == null ? 1 : user.getTokenVersion())
                .issuedAt(new Date(now))
                .expiration(new Date(now + exp));
        if (user.getEmployeeId() != null) {
            builder.claim(CLAIM_EMPLOYEE_ID, user.getEmployeeId());
        }
        if (user.getPatientId() != null) {
            builder.claim(CLAIM_PATIENT_ID, user.getPatientId());
        }
        if (user.getRealname() != null) {
            builder.claim(CLAIM_REALNAME, user.getRealname());
        }
        return builder.signWith(key).compact();
    }

    /**
     * 解析并校验 JWT v2：签名、过期、ver=2、role 合法枚举。
     * 缺失 ver 或旧 employee-only Token 一律视为无效。
     *
     * @throws ExpiredJwtException 已过期
     * @throws JwtException        签名无效 / 格式错误 / 非 v2 / role 非法
     */
    public AuthUser parse(String token) {
        Claims payload = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // ver 必须存在且等于 2（旧 employee-only Token 无 ver，直接拒绝）
        Object verRaw = payload.get(CLAIM_VER);
        if (!(verRaw instanceof Integer) || (Integer) verRaw != VER) {
            throw new MalformedJwtException("JWT ver 不合法或缺失，仅接受 v2");
        }

        String roleStr = payload.get(CLAIM_ROLE, String.class);
        Role role = Role.fromString(roleStr);
        if (role == null) {
            throw new MalformedJwtException("JWT role 非法");
        }

        Integer accountId = parseInteger(payload.getSubject());
        Integer employeeId = readInt(payload, CLAIM_EMPLOYEE_ID);
        Integer patientId = readInt(payload, CLAIM_PATIENT_ID);
        Integer tokenVersion = readInt(payload, CLAIM_TOKEN_VERSION);
        String realname = payload.get(CLAIM_REALNAME, String.class);

        return AuthUser.builder()
                .accountId(accountId)
                .role(role)
                .employeeId(employeeId)
                .patientId(patientId)
                .tokenVersion(tokenVersion)
                .realname(realname)
                .build();
    }

    public long getExpireMillis() {
        return expireHours * 3600_000L;
    }

    public boolean isExpired(String token) {
        try {
            parseSignedClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /** Token 剩余有效秒数（用于登出黑名单 TTL）。无效/过期返回 0。 */
    public long remainingSeconds(String token) {
        try {
            Date exp = parseSignedClaims(token).getExpiration();
            return Math.max(0, (exp.getTime() - System.currentTimeMillis()) / 1000);
        } catch (ExpiredJwtException e) {
            return 0;
        } catch (JwtException e) {
            return 0;
        }
    }

    private Claims parseSignedClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static Integer parseInteger(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return Integer.valueOf(s);
    }

    private static Integer readInt(Claims payload, String key) {
        Object v = payload.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return Integer.valueOf(v.toString());
    }

    private static boolean looksLikeBase64(String s) {
        if (s == null || s.length() < 8) return false;
        return s.matches("^[A-Za-z0-9+/]+={0,2}$") && s.length() % 4 == 0;
    }
}
