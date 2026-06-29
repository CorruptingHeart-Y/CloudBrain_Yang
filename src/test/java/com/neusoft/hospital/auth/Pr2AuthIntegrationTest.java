package com.neusoft.hospital.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.hospital.auth.context.AuthUser;
import com.neusoft.hospital.auth.jwt.JwtUtil;
import com.neusoft.hospital.entity.UserAccount;
import com.neusoft.hospital.mapper.UserAccountMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PR2 端到端集成验证（本地隔离库 hospital_rbac_v11 + 本地 Redis，绝不连远程）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Pr2AuthIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UserAccountMapper userAccountMapper;
    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    org.springframework.data.redis.core.StringRedisTemplate redis;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void resetState() throws Exception {
        // 使用 Apache HttpComponents 替代 JDK HttpURLConnection，避免 POST 返回 401 时
        // JDK 的 "cannot retry due to server authentication, in streaming mode" 重试异常
        rest.getRestTemplate().setRequestFactory(
                new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
        // 1. 重置 DB fixture（幂等，含将 patient01 密码重置回 MD5）；UTF-8 读取避免中文乱码
        try (Connection conn = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(conn,
                    new EncodedResource(new ClassPathResource("sql/rbac_fixture.sql"), StandardCharsets.UTF_8));
        }
        // 2. 清空 Redis（失败计数 / 黑名单），保证用例间隔离
        redis.execute((org.springframework.data.redis.core.RedisCallback<Object>) c -> {
            c.flushDb();
            return null;
        });
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private JsonNode login(String username, String password) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(mapper.writeValueAsString(Map.of("username", username, "password", password)), h);
        ResponseEntity<String> resp = rest.postForEntity(url("/api/v1/auth/login"), req, String.class);
        assertEquals(200, resp.getStatusCode().value(), "登录应返回 200");
        JsonNode body = mapper.readTree(resp.getBody());
        assertEquals(200, body.get("code").asInt(), "登录 Result.code 应为 200");
        return body;
    }

    private String token(JsonNode loginBody) {
        return loginBody.get("data").get("token").asText();
    }

    private ResponseEntity<String> getWithToken(String path, String token) {
        return getWithToken(path, token, null);
    }

    private ResponseEntity<String> getWithToken(String path, String token, String xRole) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) {
            h.set("Authorization", "Bearer " + token);
        }
        if (xRole != null) {
            h.set("X-Role", xRole);
        }
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(h), String.class);
    }

    private SecretKey sameKey() {
        // 与 JwtUtil.init 一致：secret 非 base64 → 原始字节
        String secret = "dev-only-secret-please-change-in-prod-32bytes-min!";
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ---------- 1. 三种账号登录 ----------

    @Test
    @DisplayName("ADMIN 登录成功，返回 role=ADMIN，employeeId/patientId 为 null")
    void adminLogin() throws Exception {
        JsonNode body = login("admin", "admin123");
        JsonNode info = body.get("data").get("userInfo");
        assertEquals("ADMIN", info.get("role").asText());
        assertEquals(1, info.get("accountId").asInt());
        assertTrue(info.get("employeeId").isNull(), "ADMIN 无关联 employee");
        assertTrue(info.get("patientId").isNull());
        // ADMIN 无关联 employee → 医生字段为 null，不报错
        assertTrue(info.get("deptName").isNull());
    }

    @Test
    @DisplayName("DOCTOR 登录成功，返回 role=DOCTOR，employeeId=1，含科室/挂号级别")
    void doctorLogin() throws Exception {
        JsonNode body = login("doctor01", "doctor123");
        JsonNode info = body.get("data").get("userInfo");
        assertEquals("DOCTOR", info.get("role").asText());
        assertEquals(2, info.get("accountId").asInt());
        assertEquals(1, info.get("employeeId").asInt());
        assertEquals("测试医生", info.get("realname").asText());
        assertEquals("神经内科", info.get("deptName").asText());
        assertEquals("普通号", info.get("registLevelName").asText());
    }

    @Test
    @DisplayName("PATIENT 登录成功，返回 role=PATIENT，patientId=1")
    void patientLogin() throws Exception {
        JsonNode body = login("patient01", "patient123");
        JsonNode info = body.get("data").get("userInfo");
        assertEquals("PATIENT", info.get("role").asText());
        assertEquals(3, info.get("accountId").asInt());
        assertEquals(1, info.get("patientId").asInt());
        assertEquals("测试患者", info.get("realname").asText());
        // 患者医生专属字段为 null，且不含敏感字段
        assertTrue(info.get("deptName").isNull());
        assertNull(info.get("cardNumber"));
        assertNull(info.get("phone"));
        assertNull(info.get("homeAddress"));
    }

    // ---------- 2. JWT v2 claims 含 ver=2 / accountId / role / employeeId / patientId ----------

    @Test
    @DisplayName("DOCTOR 的 JWT 解码含 ver=2、accountId、role、employeeId")
    void jwtClaimsContainVer2() throws Exception {
        String token = token(login("doctor01", "doctor123"));
        AuthUser user = jwtUtil.parse(token);
        assertEquals(2, JwtUtil.VER);
        assertEquals(2, user.getAccountId());
        assertEquals(com.neusoft.hospital.auth.enums.Role.DOCTOR, user.getRole());
        assertEquals(1, user.getEmployeeId());
        assertNull(user.getPatientId());
    }

    @Test
    @DisplayName("PATIENT 的 JWT 解码含 patientId")
    void jwtClaimsPatientId() throws Exception {
        String token = token(login("patient01", "patient123"));
        AuthUser user = jwtUtil.parse(token);
        assertEquals(1, user.getPatientId());
        assertEquals(com.neusoft.hospital.auth.enums.Role.PATIENT, user.getRole());
    }

    // ---------- 3. /auth/me 返回正确角色 ----------

    @Test
    @DisplayName("/auth/me 返回当前用户角色")
    void meReturnsRole() throws Exception {
        String token = token(login("doctor01", "doctor123"));
        ResponseEntity<String> resp = getWithToken("/api/v1/auth/me", token);
        assertEquals(200, resp.getStatusCode().value());
        JsonNode body = mapper.readTree(resp.getBody());
        assertEquals(200, body.get("code").asInt());
        assertEquals("DOCTOR", body.get("data").get("role").asText());
        assertEquals(2, body.get("data").get("accountId").asInt());
    }

    // ---------- 4. 无 Token / 旧 Token / 伪造 Token / 黑名单 Token → 401 ----------

    @Test
    @DisplayName("无 Token 访问受保护接口 → 真实 HTTP 401，Result.code=401")
    void noTokenReturns401() throws Exception {
        ResponseEntity<String> resp = getWithToken("/api/v1/auth/me", null);
        assertEquals(401, resp.getStatusCode().value());
        JsonNode body = mapper.readTree(resp.getBody());
        assertEquals(401, body.get("code").asInt());
    }

    @Test
    @DisplayName("伪造（签名错误）Token → HTTP 401")
    void forgedTokenReturns401() throws Exception {
        // 用不同密钥签名，结构合法但签名不匹配
        SecretKey wrongKey = Keys.hmacShaKeyFor("another-secret-32-bytes-long-xxxxxxx!".getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder()
                .subject("1").claim("ver", 2).claim("role", "ADMIN")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 3600_000L))
                .signWith(wrongKey).compact();
        ResponseEntity<String> resp = getWithToken("/api/v1/auth/me", forged);
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    @DisplayName("旧 employee-only Token（无 ver）→ HTTP 401")
    void oldEmployeeTokenReturns401() throws Exception {
        // 模拟 PR1 之前签发的 v1 Token：sub=employeeId，无 ver/role
        String oldToken = Jwts.builder()
                .subject("1")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 3600_000L))
                .signWith(sameKey()).compact();
        ResponseEntity<String> resp = getWithToken("/api/v1/auth/me", oldToken);
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    @DisplayName("黑名单 Token（登出后）→ HTTP 401")
    void blacklistedTokenReturns401() throws Exception {
        String token = token(login("doctor01", "doctor123"));
        // 登出
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + token);
        ResponseEntity<String> logout = rest.exchange(url("/api/v1/auth/logout"), HttpMethod.POST, new HttpEntity<>(h), String.class);
        assertEquals(200, logout.getStatusCode().value());
        // 原 Token 立即不可用
        ResponseEntity<String> resp = getWithToken("/api/v1/auth/me", token);
        assertEquals(401, resp.getStatusCode().value());
    }

    // ---------- 5. 停用账号 → 401 ----------

    @Test
    @DisplayName("停用账号后请求 → HTTP 401")
    void disabledAccountReturns401() throws Exception {
        String token = token(login("admin", "admin123"));
        // 停用 admin
        jdbc.update("UPDATE user_account SET status=0 WHERE username='admin'");
        ResponseEntity<String> resp = getWithToken("/api/v1/auth/me", token);
        assertEquals(401, resp.getStatusCode().value());
    }

    // ---------- 6. @RequireRole(ADMIN) 角色授权 ----------

    @Test
    @DisplayName("ADMIN 访问 @RequireRole(ADMIN) 接口 → 200")
    void adminAccessAdminEndpoint() throws Exception {
        String token = token(login("admin", "admin123"));
        ResponseEntity<String> resp = getWithToken("/api/v1/test/admin", token);
        assertEquals(200, resp.getStatusCode().value());
        JsonNode body = mapper.readTree(resp.getBody());
        assertEquals("admin-ok", body.get("data").asText());
    }

    @Test
    @DisplayName("DOCTOR 访问 @RequireRole(ADMIN) 接口 → 真实 HTTP 403，Result.code=403")
    void doctorForbidden() throws Exception {
        String token = token(login("doctor01", "doctor123"));
        ResponseEntity<String> resp = getWithToken("/api/v1/test/admin", token);
        assertEquals(403, resp.getStatusCode().value());
        JsonNode body = mapper.readTree(resp.getBody());
        assertEquals(403, body.get("code").asInt());
    }

    @Test
    @DisplayName("PATIENT 访问 @RequireRole(ADMIN) 接口 → HTTP 403")
    void patientForbidden() throws Exception {
        String token = token(login("patient01", "patient123"));
        ResponseEntity<String> resp = getWithToken("/api/v1/test/admin", token);
        assertEquals(403, resp.getStatusCode().value());
    }

    @Test
    @DisplayName("X-Role: ADMIN 伪造不能让 PATIENT 越权（仍 403）")
    void xRoleSpoofingDoesNotGrant() throws Exception {
        String token = token(login("patient01", "patient123"));
        ResponseEntity<String> resp = getWithToken("/api/v1/test/admin", token, "ADMIN");
        assertEquals(403, resp.getStatusCode().value());
    }

    // ---------- 7. MD5 密码首次登录后升级为 BCrypt ----------

    @Test
    @DisplayName("PATIENT 的 MD5 密码首次登录后升级为 BCrypt")
    void md5UpgradedToBcrypt() throws Exception {
        // 登录前应为 MD5（32 位 hex）
        UserAccount before = userAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAccount>()
                        .eq(UserAccount::getUsername, "patient01"));
        assertEquals(32, before.getPassword().length(), "fixture 应为 MD5");
        assertTrue(before.getPassword().matches("^[0-9a-fA-F]{32}$"));

        // 执行登录（触发升级）
        login("patient01", "patient123");

        UserAccount after = userAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAccount>()
                        .eq(UserAccount::getUsername, "patient01"));
        assertTrue(after.getPassword().startsWith("$2a$") && after.getPassword().length() == 60,
                "升级后应为 BCrypt（$2a$ 前缀，60 位）");
        // employee.password 不应被改写
        String empPwd = jdbc.queryForObject("SELECT password FROM employee WHERE id=1", String.class);
        assertEquals("$2a$10$placeholder-not-used-employee-password", empPwd);
    }

    // ---------- 8. 登录失败统一安全消息 ----------

    @Test
    @DisplayName("不存在/密码错误账号登录 → 401 且不泄露具体原因")
    void badCredentialsSafeMessage() throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(mapper.writeValueAsString(Map.of("username", "patient01", "password", "wrong")), h);
        ResponseEntity<String> resp = rest.postForEntity(url("/api/v1/auth/login"), req, String.class);
        assertEquals(401, resp.getStatusCode().value());
        JsonNode body = mapper.readTree(resp.getBody());
        assertEquals(401, body.get("code").asInt());
        String msg = body.get("message").asText();
        assertFalse(msg.contains("患者") || msg.contains("禁用") || msg.contains("patient"), "消息不应泄露具体原因: " + msg);
    }
}
