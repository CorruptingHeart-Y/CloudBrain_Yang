package com.neusoft.hospital.doctor;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PR4 管理员/医生权限矩阵 + 医生数据范围隔离 端到端验证（本地隔离库，绝不连远程）。
 * <p>
 * 双医生场景：doctor01(employee_id=1，接诊 register 1/2/3) vs doctor02(employee_id=2，接诊 register 10)。
 * 验证模块级 @RequireRole 与 Service 层 register.employee_id 归属隔离。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Pr4PermissionIntegrationTest {

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
            ScriptUtils.executeSqlScript(conn,
                    new EncodedResource(new ClassPathResource("sql/rbac_fixture.sql"), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(conn,
                    new EncodedResource(new ClassPathResource("sql/pr3_patient_fixture.sql"), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(conn,
                    new EncodedResource(new ClassPathResource("sql/pr4_doctor_fixture.sql"), StandardCharsets.UTF_8));
        }
        redis.execute((org.springframework.data.redis.core.RedisCallback<Object>) c -> {
            c.flushDb();
            return null;
        });
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String login(String username, String password) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(
                mapper.writeValueAsString(Map.of("username", username, "password", password)), h);
        ResponseEntity<String> resp = rest.postForEntity(url("/api/v1/auth/login"), req, String.class);
        assertEquals(200, resp.getStatusCode().value(), "登录应返回 200: " + resp.getBody());
        return mapper.readTree(resp.getBody()).get("data").get("token").asText();
    }

    private ResponseEntity<String> request(HttpMethod method, String path, String token, String xRole, Object body) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.set("Authorization", "Bearer " + token);
        if (xRole != null) h.set("X-Role", xRole);
        String json = body == null ? null : mapper.writeValueAsString(body);
        return rest.exchange(url(path), method, new HttpEntity<>(json, h), String.class);
    }

    private ResponseEntity<String> get(String path, String token) throws Exception {
        return request(HttpMethod.GET, path, token, null, null);
    }

    private ResponseEntity<String> get(String path, String token, String xRole) throws Exception {
        return request(HttpMethod.GET, path, token, xRole, null);
    }

    private ResponseEntity<String> post(String path, String token, Object body) throws Exception {
        return request(HttpMethod.POST, path, token, null, body);
    }

    private ResponseEntity<String> put(String path, String token, Object body) throws Exception {
        return request(HttpMethod.PUT, path, token, null, body);
    }

    private JsonNode body(ResponseEntity<String> resp) throws Exception {
        return mapper.readTree(resp.getBody());
    }

    private static final List<String> ADMIN_ONLY_PATHS = List.of(
            "/api/v1/employee", "/api/v1/department", "/api/v1/scheduling",
            "/api/v1/regist-level", "/api/v1/settle-category");

    // ---------- 1. ADMIN 访问基础管理接口 ----------

    @Test
    @DisplayName("ADMIN 可访问 employee/department/scheduling 基础管理接口")
    void adminAccessBasicManagement() throws Exception {
        String token = login("admin", "admin123");
        for (String p : List.of("/api/v1/employee", "/api/v1/department", "/api/v1/scheduling")) {
            ResponseEntity<String> resp = get(p, token);
            assertEquals(200, resp.getStatusCode().value(), p + " 应 200");
        }
    }

    // ---------- 2. DOCTOR 访问仅ADMIN接口 → 403 ----------

    @Test
    @DisplayName("DOCTOR 访问 employee/department/scheduling/regist-level/settle-category 均为 HTTP 403")
    void doctorForbiddenOnAdminOnly() throws Exception {
        String token = login("doctor01", "doctor123");
        for (String p : ADMIN_ONLY_PATHS) {
            ResponseEntity<String> resp = get(p, token);
            assertEquals(403, resp.getStatusCode().value(), p + " 应 403");
            assertEquals(403, body(resp).get("code").asInt(), p + " Result.code 应 403");
        }
    }

    // ---------- 3. PATIENT 继续无法访问旧通用接口 ----------

    @Test
    @DisplayName("PATIENT 访问 prescription/check 等旧通用接口仍为 HTTP 403（/register GET 已允许只读）")
    void patientForbiddenOnLegacy() throws Exception {
        String token = login("patient01", "patient123");
        // v2.0：PATIENT GET /register 已允许（只读本人 link）；其余旧通用接口仍 403
        for (String p : List.of("/api/v1/prescription", "/api/v1/check-request",
                "/api/v1/inspection-request", "/api/v1/disposal-request", "/api/v1/employee")) {
            ResponseEntity<String> resp = get(p, token);
            assertEquals(403, resp.getStatusCode().value(), p + " 应 403");
        }
    }

    // ---------- 4 & 5. DOCTOR A 查看自己 / B 的 register ----------

    @Test
    @DisplayName("DOCTOR A 查看自己的 register(id=1) 返回 200；查看 DOCTOR B 的 register(id=10) 返回真实 HTTP 404")
    void doctorViewOwnAndOthersRegister() throws Exception {
        String token = login("doctor01", "doctor123");
        ResponseEntity<String> own = get("/api/v1/register/1", token);
        assertEquals(200, own.getStatusCode().value());
        assertEquals(1, body(own).get("data").get("id").asInt());

        ResponseEntity<String> others = get("/api/v1/register/10", token);
        assertEquals(404, others.getStatusCode().value(), "他人 register 应真实 HTTP 404");
        assertEquals(404, body(others).get("code").asInt());
    }

    @Test
    @DisplayName("DOCTOR B 查看自己的 register(id=10) 200；查看 A 的 register(id=1) 404")
    void doctorBViewOwnAndOthersRegister() throws Exception {
        String token = login("doctor02", "doctor123");
        assertEquals(200, get("/api/v1/register/10", token).getStatusCode().value());
        assertEquals(404, get("/api/v1/register/1", token).getStatusCode().value());
    }

    @Test
    @DisplayName("DOCTOR 分页列表只含本人接诊 register：A 不含 10，B 不含 1")
    void doctorPageListScoped() throws Exception {
        String tokenA = login("doctor01", "doctor123");
        JsonNode a = body(get("/api/v1/register", tokenA)).get("data");
        Set<Integer> aIds = StreamSupport.stream(a.get("records").spliterator(), false)
                .map(n -> n.get("id").asInt()).collect(Collectors.toSet());
        assertFalse(aIds.contains(10), "医生A列表不得含医生B的 register 10");
        assertTrue(aIds.contains(1), "医生A列表应含自己的 register 1");

        String tokenB = login("doctor02", "doctor123");
        JsonNode b = body(get("/api/v1/register", tokenB)).get("data");
        Set<Integer> bIds = StreamSupport.stream(b.get("records").spliterator(), false)
                .map(n -> n.get("id").asInt()).collect(Collectors.toSet());
        assertFalse(bIds.contains(1), "医生B列表不得含医生A的 register 1");
        assertTrue(bIds.contains(10), "医生B列表应含自己的 register 10");
    }

    // ---------- 6. DOCTOR A 为自己 register 创建/修改派生记录 ----------

    @Test
    @DisplayName("DOCTOR A 为自己 register(id=1) 创建处方/检查/检验/处置 成功；修改自己处方/检查 成功")
    void doctorCreateModifyOwn() throws Exception {
        String token = login("doctor01", "doctor123");
        assertEquals(200, post("/api/v1/prescription", token,
                Map.of("registerId", 1, "drugId", 1, "drugUsage", "A方", "drugNumber", 5)).getStatusCode().value());
        assertEquals(200, post("/api/v1/check-request", token,
                Map.of("registerId", 1, "medicalTechnologyId", 1, "checkInfo", "A检查", "checkPosition", "头")).getStatusCode().value());
        assertEquals(200, post("/api/v1/inspection-request", token,
                Map.of("registerId", 1, "medicalTechnologyId", 1, "inspectionInfo", "A检验", "inspectionPosition", "血")).getStatusCode().value());
        assertEquals(200, post("/api/v1/disposal-request", token,
                Map.of("registerId", 1, "medicalTechnologyId", 1, "disposalInfo", "A处置", "disposalPosition", "手")).getStatusCode().value());

        // 修改自己名下(register 1)的处方 id=1 / 检查 id=1
        assertEquals(200, put("/api/v1/prescription", token, Map.of("id", 1, "drugState", "1")).getStatusCode().value());
        assertEquals(200, put("/api/v1/check-request", token, Map.of("id", 1, "checkState", "1")).getStatusCode().value());
    }

    // ---------- 7. DOCTOR A 对 B 的 register 创建/修改/查询 → 404 ----------

    @Test
    @DisplayName("DOCTOR A 对 B 的 register(id=10) 创建处方/检查/检验/处置 均为真实 HTTP 404")
    void doctorCreateOnOthersRegister404() throws Exception {
        String token = login("doctor01", "doctor123");
        String[] paths = {"/api/v1/prescription", "/api/v1/check-request", "/api/v1/inspection-request", "/api/v1/disposal-request"};
        for (String p : paths) {
            ResponseEntity<String> resp = post(p, token,
                    Map.of("registerId", 10, "medicalTechnologyId", 1, "drugId", 1));
            assertEquals(404, resp.getStatusCode().value(), p + " 创建他人register应 404");
            assertEquals(404, body(resp).get("code").asInt());
        }
    }

    @Test
    @DisplayName("DOCTOR A 修改/查询 B 名下记录(prescription 20 / check 20) 与 listByRegisterId(10) 均为真实 HTTP 404")
    void doctorModifyQueryOthersRecords404() throws Exception {
        String token = login("doctor01", "doctor123");
        // 修改 B 名下处方/检查 → 404
        assertEquals(404, put("/api/v1/prescription", token, Map.of("id", 20, "drugState", "1")).getStatusCode().value());
        assertEquals(404, put("/api/v1/check-request", token, Map.of("id", 20, "checkState", "1")).getStatusCode().value());
        // 查询 B 名下记录 → 404
        assertEquals(404, get("/api/v1/prescription/20", token).getStatusCode().value());
        assertEquals(404, get("/api/v1/check-request/20", token).getStatusCode().value());
        // 列出 B 的 register 派生记录 → 404
        assertEquals(404, get("/api/v1/prescription/register/10", token).getStatusCode().value());
        assertEquals(404, get("/api/v1/check-request/register/10", token).getStatusCode().value());
    }


    @Test
    @DisplayName("DOCTOR A 查询 AI 病历/处方审核记录：自己 200，他人 register 404")
    void doctorQueryAiReadRecordsScopedByRegisterOwnership() throws Exception {
        String token = login("doctor01", "doctor123");

        ResponseEntity<String> ownMedicalRecord = get("/api/v1/medical-record?registerId=1", token);
        assertEquals(200, ownMedicalRecord.getStatusCode().value());
        assertEquals(1, body(ownMedicalRecord).get("data").get("registerId").asInt());

        ResponseEntity<String> othersMedicalRecord = get("/api/v1/medical-record?registerId=10", token);
        assertEquals(404, othersMedicalRecord.getStatusCode().value(), "AI medical-record GET must hide other doctor's register");
        assertEquals(404, body(othersMedicalRecord).get("code").asInt());

        ResponseEntity<String> ownAuditRecords = get("/api/v1/prescription/check/record?registerId=1", token);
        assertEquals(200, ownAuditRecords.getStatusCode().value());
        assertEquals(1, body(ownAuditRecords).get("data").get(0).get("registerId").asInt());

        ResponseEntity<String> othersAuditRecords = get("/api/v1/prescription/check/record?registerId=10", token);
        assertEquals(404, othersAuditRecords.getStatusCode().value(), "AI prescription audit record GET must hide other doctor's register");
        assertEquals(404, body(othersAuditRecords).get("code").asInt());
    }
    // ---------- 8. ADMIN 跨医生访问与操作 ----------

    @Test
    @DisplayName("ADMIN 可跨医生查看 register 1/10，并为 register 10 创建/修改记录")
    void adminCrossDoctorAccess() throws Exception {
        String token = login("admin", "admin123");
        assertEquals(200, get("/api/v1/register/1", token).getStatusCode().value());
        assertEquals(200, get("/api/v1/register/10", token).getStatusCode().value());
        // 查看医生B名下记录
        assertEquals(200, get("/api/v1/prescription/20", token).getStatusCode().value());
        assertEquals(200, get("/api/v1/check-request/register/10", token).getStatusCode().value());
        // 为医生B的register创建处方 + 修改B名下处方
        assertEquals(200, post("/api/v1/prescription", token,
                Map.of("registerId", 10, "drugId", 1, "drugUsage", "admin跨医生", "drugNumber", 3)).getStatusCode().value());
        assertEquals(200, put("/api/v1/prescription", token, Map.of("id", 20, "drugState", "1")).getStatusCode().value());
    }

    // ---------- 9. X-Role 伪造无效 ----------

    @Test
    @DisplayName("X-Role: ADMIN 不能让 DOCTOR 越权（他人register仍404，仅ADMIN接口仍403）")
    void xRoleSpoofingNoEffectOnDoctor() throws Exception {
        String token = login("doctor01", "doctor123");
        assertEquals(404, get("/api/v1/register/10", token, "ADMIN").getStatusCode().value(), "X-Role伪造不能让医生看他人register");
        assertEquals(403, get("/api/v1/employee", token, "ADMIN").getStatusCode().value(), "X-Role伪造不能让医生访问ADMIN接口");
    }

    @Test
    @DisplayName("X-Role: ADMIN 不能让 PATIENT 越权访问 ADMIN 专属接口")
    void xRoleSpoofingNoEffectOnPatient() throws Exception {
        String token = login("patient01", "patient123");
        assertEquals(403, get("/api/v1/employee", token, "ADMIN").getStatusCode().value());
    }

    // ---------- 附加：DOCTOR 访问 drug-info/disease/medical-technology (ADMIN+DOCTOR) 应 200 ----------

    @Test
    @DisplayName("DOCTOR 可访问 drug-info/disease/medical-technology（ADMIN+DOCTOR 共享字典）")
    void doctorAccessSharedDictionary() throws Exception {
        String token = login("doctor01", "doctor123");
        for (String p : List.of("/api/v1/drug-info", "/api/v1/disease", "/api/v1/medical-technology")) {
            assertEquals(200, get(p, token).getStatusCode().value(), p + " 应 200");
        }
    }
}
