package com.neusoft.hospital.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PR5 账号管理 + 患者注册 + token_version 全局失效 端到端验证（本地隔离库，绝不连远程）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Pr5AccountIntegrationTest {

    @LocalServerPort
    int port;
    @Autowired
    TestRestTemplate rest;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    org.springframework.data.redis.core.StringRedisTemplate redis;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void resetState() throws Exception {
        rest.getRestTemplate().setRequestFactory(
                new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
        try (Connection conn = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(conn, new EncodedResource(new ClassPathResource("sql/rbac_fixture.sql"), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(conn, new EncodedResource(new ClassPathResource("sql/pr3_patient_fixture.sql"), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(conn, new EncodedResource(new ClassPathResource("sql/pr4_doctor_fixture.sql"), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(conn, new EncodedResource(new ClassPathResource("sql/pr5_account_fixture.sql"), StandardCharsets.UTF_8));
        }
        redis.execute((org.springframework.data.redis.core.RedisCallback<Object>) c -> { c.flushDb(); return null; });
    }

    private String url(String p) { return "http://localhost:" + port + p; }

    private ResponseEntity<String> loginRaw(String username, String password) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(url("/api/v1/auth/login"),
                new HttpEntity<>(mapper.writeValueAsString(Map.of("username", username, "password", password)), h), String.class);
    }

    private String login(String username, String password) throws Exception {
        ResponseEntity<String> resp = loginRaw(username, password);
        assertEquals(200, resp.getStatusCode().value(), "登录应200: " + resp.getBody());
        return mapper.readTree(resp.getBody()).get("data").get("token").asText();
    }

    private ResponseEntity<String> req(HttpMethod m, String path, String token, String xRole, Object body) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.set("Authorization", "Bearer " + token);
        if (xRole != null) h.set("X-Role", xRole);
        return rest.exchange(url(path), m, new HttpEntity<>(body == null ? null : mapper.writeValueAsString(body), h), String.class);
    }

    private JsonNode body(ResponseEntity<String> r) throws Exception { return mapper.readTree(r.getBody()); }

    private int createEmployeeAccount(String token, String username, String role, int employeeId, String password) throws Exception {
        ResponseEntity<String> r = req(HttpMethod.POST, "/api/v1/account/employee", token, null,
                Map.of("username", username, "password", password, "role", role, "employeeId", employeeId));
        assertEquals(200, r.getStatusCode().value(), "创建员工账号应200: " + r.getBody());
        return body(r).get("data").get("accountId").asInt();
    }

    // ---------- 1 & 3. 创建 DOCTOR 账号 + 重复绑定失败 ----------

    @Test
    @DisplayName("ADMIN 创建 DOCTOR 账号并绑定 employee 成功；同 employeeId 再次绑定 409")
    void createDoctorAccountAndDuplicateBind() throws Exception {
        String admin = login("admin", "admin123");
        int id = createEmployeeAccount(admin, "doc_acct_a", "DOCTOR", 3, "pass123");
        assertTrue(id > 0);
        // 账号响应不含敏感字段
        JsonNode data = body(req(HttpMethod.GET, "/api/v1/account/" + id, admin, null, null)).get("data");
        assertFalse(data.has("password"));
        // 同 employeeId 再次绑定 → 409
        ResponseEntity<String> dup = req(HttpMethod.POST, "/api/v1/account/employee", admin, null,
                Map.of("username", "doc_acct_a_dup", "password", "pass123", "role", "DOCTOR", "employeeId", 3));
        assertEquals(409, dup.getStatusCode().value());
        assertEquals(409, body(dup).get("code").asInt());
    }

    // ---------- 2 & 3. 创建 PATIENT 账号 + 重复绑定失败 ----------

    @Test
    @DisplayName("ADMIN 创建 PATIENT 账号并绑定 patient 成功；同 patientId 再次绑定 409")
    void createPatientAccountAndDuplicateBind() throws Exception {
        String admin = login("admin", "admin123");
        ResponseEntity<String> r = req(HttpMethod.POST, "/api/v1/account/patient", admin, null,
                Map.of("username", "pat_acct_b", "password", "pass123", "patientId", 3));
        assertEquals(200, r.getStatusCode().value());
        // 同 patientId 再次绑定 → 409
        ResponseEntity<String> dup = req(HttpMethod.POST, "/api/v1/account/patient", admin, null,
                Map.of("username", "pat_acct_b_dup", "password", "pass123", "patientId", 3));
        assertEquals(409, dup.getStatusCode().value());
    }

    // ---------- 禁止经员工接口创建 PATIENT ----------

    @Test
    @DisplayName("ADMIN 经 /account/employee 创建 PATIENT 被拒(Result.code=400)")
    void cannotCreatePatientViaEmployeeEndpoint() throws Exception {
        String admin = login("admin", "admin123");
        ResponseEntity<String> r = req(HttpMethod.POST, "/api/v1/account/employee", admin, null,
                Map.of("username", "bad_pat", "password", "pass123", "role", "PATIENT", "employeeId", 4));
        // PARAM_ERROR 按项目设计为 HTTP 200 + Result.code=400
        assertEquals(400, body(r).get("code").asInt());
        Integer cnt = jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE username='bad_pat'", Integer.class);
        assertEquals(0, cnt, "不应创建 PATIENT 账号");
    }

    // ---------- 4. DOCTOR/PATIENT 访问 /account → 403 ----------

    @Test
    @DisplayName("DOCTOR/PATIENT 访问 /api/v1/account/** 真实 HTTP 403")
    void nonAdminForbiddenOnAccountApi() throws Exception {
        String doc = login("doctor01", "doctor123");
        String pat = login("patient01", "patient123");
        assertEquals(403, req(HttpMethod.GET, "/api/v1/account", doc, null, null).getStatusCode().value());
        assertEquals(403, req(HttpMethod.GET, "/api/v1/account", pat, null, null).getStatusCode().value());
    }

    // ---------- 5. 账号查询不含敏感字段 ----------

    @Test
    @DisplayName("账号列表/详情不返回 password/cardNumber/phone/homeAddress")
    void accountQueryNoSensitiveFields() throws Exception {
        String admin = login("admin", "admin123");
        JsonNode list = body(req(HttpMethod.GET, "/api/v1/account", admin, null, null)).get("data");
        assertTrue(list.get("records").size() >= 1);
        JsonNode first = list.get("records").get(0);
        for (String k : new String[]{"password", "cardNumber", "phone", "homeAddress"}) {
            assertFalse(first.has(k), "列表不得含 " + k);
        }
        JsonNode detail = body(req(HttpMethod.GET, "/api/v1/account/1", admin, null, null)).get("data");
        for (String k : new String[]{"password", "cardNumber", "phone", "homeAddress"}) {
            assertFalse(detail.has(k), "详情不得含 " + k);
        }
    }

    // ---------- 6. 禁用账号 → 旧Token 401；启用后旧Token仍401，重新登录才可用 ----------

    @Test
    @DisplayName("ADMIN 禁用账号后旧Token 401、登录失败；启用后旧Token仍401，重新登录可用")
    void disableAccountInvalidatesToken() throws Exception {
        String admin = login("admin", "admin123");
        int id = createEmployeeAccount(admin, "dis_doc", "DOCTOR", 4, "pass123");
        String t1 = login("dis_doc", "pass123");
        assertEquals(200, req(HttpMethod.GET, "/api/v1/auth/me", t1, null, null).getStatusCode().value());
        // 禁用
        assertEquals(200, req(HttpMethod.PATCH, "/api/v1/account/" + id + "/status", admin, null, Map.of("status", 0)).getStatusCode().value());
        assertEquals(401, req(HttpMethod.GET, "/api/v1/auth/me", t1, null, null).getStatusCode().value());
        assertEquals(401, loginRaw("dis_doc", "pass123").getStatusCode().value());
        // 启用
        assertEquals(200, req(HttpMethod.PATCH, "/api/v1/account/" + id + "/status", admin, null, Map.of("status", 1)).getStatusCode().value());
        assertEquals(401, req(HttpMethod.GET, "/api/v1/auth/me", t1, null, null).getStatusCode().value(), "启用不恢复旧Token");
        // 重新登录可用
        String t2 = login("dis_doc", "pass123");
        assertEquals(200, req(HttpMethod.GET, "/api/v1/auth/me", t2, null, null).getStatusCode().value());
    }

    // ---------- 7. 重置密码 → 旧Token 401、旧密码失败、新密码成功 ----------

    @Test
    @DisplayName("ADMIN 重置密码后旧Token 401、旧密码失败、新密码成功")
    void resetPasswordInvalidatesToken() throws Exception {
        String admin = login("admin", "admin123");
        int id = createEmployeeAccount(admin, "reset_doc", "DOCTOR", 5, "oldpass123");
        String t = login("reset_doc", "oldpass123");
        assertEquals(200, req(HttpMethod.POST, "/api/v1/account/" + id + "/reset-password", admin, null, Map.of("newPassword", "newpass123")).getStatusCode().value());
        assertEquals(401, req(HttpMethod.GET, "/api/v1/auth/me", t, null, null).getStatusCode().value());
        assertEquals(401, loginRaw("reset_doc", "oldpass123").getStatusCode().value());
        assertEquals(200, loginRaw("reset_doc", "newpass123").getStatusCode().value());
    }

    // ---------- 8. 用户自行改密码 → 当前Token 401、旧密码失败、新密码成功 ----------

    @Test
    @DisplayName("用户自行改密码后当前Token 401、旧密码失败、新密码成功")
    void selfChangePasswordInvalidatesToken() throws Exception {
        String admin = login("admin", "admin123");
        createEmployeeAccount(admin, "selfpw_doc", "DOCTOR", 6, "oldpass123");
        String t = login("selfpw_doc", "oldpass123");
        assertEquals(200, req(HttpMethod.PUT, "/api/v1/auth/password", t, null,
                Map.of("oldPassword", "oldpass123", "newPassword", "newpass123")).getStatusCode().value());
        assertEquals(401, req(HttpMethod.GET, "/api/v1/auth/me", t, null, null).getStatusCode().value());
        assertEquals(401, loginRaw("selfpw_doc", "oldpass123").getStatusCode().value());
        assertEquals(200, loginRaw("selfpw_doc", "newpass123").getStatusCode().value());
    }

    // ---------- 9. 不能禁用自己 ----------

    @Test
    @DisplayName("ADMIN 不能禁用当前登录的自己(409)")
    void cannotDisableSelf() throws Exception {
        String admin = login("admin", "admin123");
        ResponseEntity<String> r = req(HttpMethod.PATCH, "/api/v1/account/1/status", admin, null, Map.of("status", 0));
        assertEquals(409, r.getStatusCode().value());
    }

    // ---------- 10. 不能禁用最后一个启用 ADMIN ----------

    @Test
    @DisplayName("不能禁用最后一个启用的 ADMIN")
    void cannotDisableLastAdmin() throws Exception {
        String admin = login("admin", "admin123");
        // 创建第二个 ADMIN（绑定 employee 7）
        int admin2 = createEmployeeAccount(admin, "admin_two", "ADMIN", 7, "pass123");
        // 禁用第二个 ADMIN（成功，剩 admin1）
        assertEquals(200, req(HttpMethod.PATCH, "/api/v1/account/" + admin2 + "/status", admin, null, Map.of("status", 0)).getStatusCode().value());
        // 此时仅 admin1 启用；禁用 admin1（自己）被拒
        assertEquals(409, req(HttpMethod.PATCH, "/api/v1/account/1/status", admin, null, Map.of("status", 0)).getStatusCode().value());
    }

    // ---------- 11. 患者自助注册新 cardNumber ----------

    @Test
    @DisplayName("患者自助注册新 cardNumber：创建 patient+账号，可登录，/auth/me 返回 PATIENT")
    void registerNewPatient() throws Exception {
        ResponseEntity<String> r = req(HttpMethod.POST, "/api/v1/auth/patient/register", null, null, Map.of(
                "username", "reg_new", "password", "reg123", "realName", "新患者",
                "gender", "男", "birthdate", "1999-09-09",
                "cardNumber", "210102199909099999", "phone", "13900000000", "homeAddress", "沈阳市新地址"));
        assertEquals(200, r.getStatusCode().value());
        String t = login("reg_new", "reg123");
        JsonNode me = body(req(HttpMethod.GET, "/api/v1/auth/me", t, null, null)).get("data");
        assertEquals("PATIENT", me.get("role").asText());
    }

    // ---------- 12. 注册已有 patient 且资料一致 → 绑定，不修改身份资料 ----------

    @Test
    @DisplayName("注册已有 patient 且资料一致：正确绑定，不修改 patient 关键身份资料")
    void registerBindExistingMatchingPatient() throws Exception {
        // patient id=5: 测试患者E / 男 / 2002-03-03 / 210102200003035555
        ResponseEntity<String> r = req(HttpMethod.POST, "/api/v1/auth/patient/register", null, null, Map.of(
                "username", "reg_bind", "password", "reg123", "realName", "测试患者E",
                "gender", "男", "birthdate", "2002-03-03",
                "cardNumber", "210102200003035555", "phone", "13900000001", "homeAddress", "不应写入"));
        assertEquals(200, r.getStatusCode().value());
        assertEquals(200, loginRaw("reg_bind", "reg123").getStatusCode().value());
        // patient 5 关键身份资料未被修改
        String realName = jdbc.queryForObject("SELECT real_name FROM patient WHERE id=5", String.class);
        assertEquals("测试患者E", realName);
    }

    // ---------- 13. 注册已有 patient 但资料不一致 → 安全失败，不创建账号 ----------

    @Test
    @DisplayName("注册已有 patient 但资料不一致：安全失败，不创建账号、不修改 patient")
    void registerExistingPatientMismatch() throws Exception {
        // patient id=4: 测试患者D / 女 / 2001-02-02 / 210102200002024444
        ResponseEntity<String> r = req(HttpMethod.POST, "/api/v1/auth/patient/register", null, null, Map.of(
                "username", "reg_mis", "password", "reg123", "realName", "错误姓名",
                "gender", "男", "birthdate", "2001-02-02",
                "cardNumber", "210102200002024444", "phone", "13900000002", "homeAddress", "x"));
        // REGISTER_FAILED 按项目设计为 HTTP 200 + Result.code=400，统一安全消息不泄露原因
        assertEquals(400, body(r).get("code").asInt());
        // 未创建账号
        Integer cnt = jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE username='reg_mis'", Integer.class);
        assertEquals(0, cnt);
    }

    // ---------- 14. 重复 username / 已绑定 patient → 安全失败，无部分写入 ----------

    @Test
    @DisplayName("重复 username / 已绑定 patient：安全失败，无部分写入")
    void registerDuplicateUsernameAndAlreadyBound() throws Exception {
        // (a) 先注册一个新 patient + 账号 dup_user
        assertEquals(200, req(HttpMethod.POST, "/api/v1/auth/patient/register", null, null, Map.of(
                "username", "dup_user", "password", "reg123", "realName", "张三",
                "gender", "男", "birthdate", "2000-01-01",
                "cardNumber", "210102200004046666", "phone", "13900000003", "homeAddress", "x")).getStatusCode().value());
        // 重复 username（不同 cardNumber）→ 安全失败
        ResponseEntity<String> dup = req(HttpMethod.POST, "/api/v1/auth/patient/register", null, null, Map.of(
                "username", "dup_user", "password", "reg123", "realName", "李四",
                "gender", "男", "birthdate", "2000-01-01",
                "cardNumber", "210102200005057777", "phone", "13900000004", "homeAddress", "x"));
        assertEquals(400, body(dup).get("code").asInt());
        // 第二个 cardNumber 的 patient 不应被创建
        Integer cnt2 = jdbc.queryForObject("SELECT COUNT(*) FROM patient WHERE card_number='210102200005057777'", Integer.class);
        assertEquals(0, cnt2, "重复username失败不应部分写入新patient");

        // (b) 已绑定 patient：注册同一 cardNumber 再次 → 安全失败
        ResponseEntity<String> bound2 = req(HttpMethod.POST, "/api/v1/auth/patient/register", null, null, Map.of(
                "username", "bound2", "password", "reg123", "realName", "张三",
                "gender", "男", "birthdate", "2000-01-01",
                "cardNumber", "210102200004046666", "phone", "13900000005", "homeAddress", "x"));
        assertEquals(400, body(bound2).get("code").asInt());
        Integer cntBound2 = jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE username='bound2'", Integer.class);
        assertEquals(0, cntBound2);
    }

    // ---------- 15. X-Role 伪造不影响 /account 403 ----------

    @Test
    @DisplayName("X-Role: ADMIN 不能让 DOCTOR/PATIENT 越权访问 /account")
    void xRoleSpoofingNoEffect() throws Exception {
        String doc = login("doctor01", "doctor123");
        String pat = login("patient01", "patient123");
        assertEquals(403, req(HttpMethod.GET, "/api/v1/account", doc, "ADMIN", null).getStatusCode().value());
        assertEquals(403, req(HttpMethod.GET, "/api/v1/account", pat, "ADMIN", null).getStatusCode().value());
    }
}
