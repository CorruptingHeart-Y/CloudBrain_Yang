package com.neusoft.hospital.patient;

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
 * PR3 患者门户端到端集成验证（本地隔离库 hospital_rbac_v11 + 本地 Redis，绝不连远程）。
 * <p>
 * 覆盖：个人资料脱敏、就诊记录归属隔离、详情归属校验(404)、参数/X-Role 无法越权、
 * 旧通用接口对 PATIENT 默认 403、ADMIN/DOCTOR 访问患者接口 403。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Pr3PatientIntegrationTest {

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
        // Apache HttpComponents 替代 JDK HttpURLConnection，规避 POST 返回 401 的重试异常
        rest.getRestTemplate().setRequestFactory(
                new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
        // 1. 先重置 PR2 基础 fixture（admin/doctor01/patient01 + 字典/员工/患者1）
        // 2. 再叠加 PR3 fixture（patientB/patient02 + register + 关联数据 + NULL patient_id 异常记录）
        try (Connection conn = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(conn,
                    new EncodedResource(new ClassPathResource("sql/rbac_fixture.sql"), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(conn,
                    new EncodedResource(new ClassPathResource("sql/pr3_patient_fixture.sql"), StandardCharsets.UTF_8));
        }
        // 3. 清空 Redis（失败计数 / 黑名单）
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
        assertEquals(200, resp.getStatusCode().value(), "登录应返回 200");
        return mapper.readTree(resp.getBody()).get("data").get("token").asText();
    }

    private ResponseEntity<String> get(String path, String token) {
        return get(path, token, null);
    }

    private ResponseEntity<String> get(String path, String token, String xRole) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) {
            h.set("Authorization", "Bearer " + token);
        }
        if (xRole != null) {
            h.set("X-Role", xRole);
        }
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(h), String.class);
    }

    private JsonNode body(ResponseEntity<String> resp) throws Exception {
        return mapper.readTree(resp.getBody());
    }

    // ---------- 1. /patient/profile 脱敏 ----------

    @Test
    @DisplayName("patientA /patient/profile 返回 200 且不含 cardNumber/phone/homeAddress")
    void profileIsDesensitized() throws Exception {
        String token = login("patient01", "patient123");
        ResponseEntity<String> resp = get("/api/v1/patient/profile", token);
        assertEquals(200, resp.getStatusCode().value());
        JsonNode data = body(resp).get("data");
        assertEquals(1, data.get("patientId").asInt());
        assertEquals("测试患者", data.get("realName").asText());
        assertFalse(data.has("cardNumber"), "不得返回身份证号");
        assertFalse(data.has("phone"), "不得返回手机号");
        assertFalse(data.has("homeAddress"), "不得返回家庭住址");
    }

    // ---------- 2. /patient/records 归属隔离 ----------

    @Test
    @DisplayName("patientA /patient/records 只返回 A 的记录，不含 B 与 patient_id IS NULL 的异常记录")
    void recordsOnlyOwn() throws Exception {
        String token = login("patient01", "patient123");
        ResponseEntity<String> resp = get("/api/v1/patient/records", token);
        assertEquals(200, resp.getStatusCode().value());
        JsonNode data = body(resp).get("data");
        assertEquals(2L, data.get("total").asLong(), "patientA 应仅有 2 条就诊记录");
        Set<Integer> registerIds = StreamSupport.stream(data.get("records").spliterator(), false)
                .map(n -> n.get("registerId").asInt())
                .collect(Collectors.toSet());
        assertEquals(Set.of(1, 2), registerIds, "仅返回 patientA 的 register 1,2");
    }

    @Test
    @DisplayName("patientB /patient/records 只返回 B 的记录（register 3）")
    void recordsOnlyOwnForB() throws Exception {
        String token = login("patient02", "patient123");
        ResponseEntity<String> resp = get("/api/v1/patient/records", token);
        assertEquals(200, resp.getStatusCode().value());
        JsonNode data = body(resp).get("data");
        assertEquals(1L, data.get("total").asLong());
        Set<Integer> registerIds = StreamSupport.stream(data.get("records").spliterator(), false)
                .map(n -> n.get("registerId").asInt())
                .collect(Collectors.toSet());
        assertEquals(Set.of(3), registerIds);
    }

    // ---------- 3. /patient/records/{ownRegisterId} 详情含关联列表 ----------

    @Test
    @DisplayName("patientA 查询自己的 registerId=1 返回 200 并含真实关联列表")
    void ownRecordDetailHasRelations() throws Exception {
        String token = login("patient01", "patient123");
        ResponseEntity<String> resp = get("/api/v1/patient/records/1", token);
        assertEquals(200, resp.getStatusCode().value());
        JsonNode data = body(resp).get("data");
        assertEquals(1, data.get("register").get("registerId").asInt());
        assertNotNull(data.get("medicalRecord"), "register 1 应有病历");
        assertEquals("偏头痛", data.get("medicalRecord").get("diagnosis").asText());
        assertTrue(data.get("prescriptions").size() >= 1, "应含至少 1 条处方");
        assertTrue(data.get("checkRequests").size() >= 1, "应含至少 1 条检查");
        assertTrue(data.get("inspectionRequests").size() >= 1, "应含至少 1 条检验");
        assertTrue(data.get("disposalRequests").size() >= 1, "应含至少 1 条处置");
        // 详情中无敏感字段
        assertFalse(data.get("register").has("cardNumber"));
        assertFalse(data.get("register").has("homeAddress"));
    }

    @Test
    @DisplayName("patientA 查询自己的 registerId=2 返回 200，关联列表可为空")
    void ownRecordDetailEmptyRelationsOk() throws Exception {
        String token = login("patient01", "patient123");
        ResponseEntity<String> resp = get("/api/v1/patient/records/2", token);
        assertEquals(200, resp.getStatusCode().value());
        JsonNode data = body(resp).get("data");
        assertTrue(data.get("prescriptions").isEmpty());
        assertTrue(data.get("medicalRecord").isNull());
    }

    // ---------- 4. 越权 registerId → 真实 HTTP 404 ----------

    @Test
    @DisplayName("patientA 查询 patientB 的 registerId=3 返回真实 HTTP 404，Result.code=404")
    void othersRegisterIdReturns404() throws Exception {
        String token = login("patient01", "patient123");
        ResponseEntity<String> resp = get("/api/v1/patient/records/3", token);
        assertEquals(404, resp.getStatusCode().value(), "应返回真实 HTTP 404");
        JsonNode b = body(resp);
        assertEquals(404, b.get("code").asInt(), "Result.code 应为 404");
        // 不透露记录是否存在：data 为空
        assertTrue(b.get("data").isNull() || b.has("data") && b.get("data").isNull());
    }

    @Test
    @DisplayName("patientA 查询不存在的 registerId=9999 返回 HTTP 404")
    void nonexistentRegisterIdReturns404() throws Exception {
        String token = login("patient01", "patient123");
        ResponseEntity<String> resp = get("/api/v1/patient/records/9999", token);
        assertEquals(404, resp.getStatusCode().value());
    }

    // ---------- 5. patientId / employeeId 参数不影响归属 ----------

    @Test
    @DisplayName("patientA 带 ?patientId=B&employeeId=doctor 仍只返回 A 的数据")
    void queryParamsCannotOverrideOwnership() throws Exception {
        String token = login("patient01", "patient123");
        ResponseEntity<String> resp = get("/api/v1/patient/records?patientId=2&employeeId=1", token);
        assertEquals(200, resp.getStatusCode().value());
        JsonNode data = body(resp).get("data");
        assertEquals(2L, data.get("total").asLong(), "参数不得改变归属，仍为 A 的 2 条");
        Set<Integer> registerIds = StreamSupport.stream(data.get("records").spliterator(), false)
                .map(n -> n.get("registerId").asInt())
                .collect(Collectors.toSet());
        assertEquals(Set.of(1, 2), registerIds);
    }

    // ---------- 6. 旧通用接口对 PATIENT 默认 403 ----------

    @Test
    @DisplayName("patientA 访问旧通用接口 register/prescription/check-request/inspection-request/disposal-request 均为 HTTP 403")
    void legacyEndpointsForbiddenForPatient() throws Exception {
        String token = login("patient01", "patient123");
        // v2.0：PATIENT GET /register 已允许（只读本人 link），不再 403；
        // 其余旧通用诊疗接口对 PATIENT 仍 403
        List<String> paths = List.of(
                "/api/v1/prescription",
                "/api/v1/check-request",
                "/api/v1/inspection-request",
                "/api/v1/disposal-request");
        for (String p : paths) {
            ResponseEntity<String> resp = get(p, token);
            assertEquals(403, resp.getStatusCode().value(), p + " 应返回 HTTP 403");
            assertEquals(403, body(resp).get("code").asInt(), p + " Result.code 应为 403");
        }
    }

    // ---------- 7. X-Role 伪造不能越权 ----------

    @Test
    @DisplayName("patientA 带 X-Role: ADMIN 仍 403/404，不能越权")
    void xRoleSpoofingDoesNotGrant() throws Exception {
        String token = login("patient01", "patient123");
        // 旧通用接口：X-Role: ADMIN 仍 403（角色以 DB 为准）
        ResponseEntity<String> legacy = get("/api/v1/prescription", token, "ADMIN");
        assertEquals(403, legacy.getStatusCode().value());
        // 他人 registerId：X-Role: ADMIN 仍 404（归属以 CurrentUser.patientId 为准）
        ResponseEntity<String> others = get("/api/v1/patient/records/3", token, "ADMIN");
        assertEquals(404, others.getStatusCode().value());
    }

    // ---------- 8. ADMIN / DOCTOR 访问患者专属接口默认 403 ----------

    @Test
    @DisplayName("ADMIN 访问 /api/v1/patient/** 默认 HTTP 403")
    void adminForbiddenOnPatientEndpoints() throws Exception {
        String token = login("admin", "admin123");
        ResponseEntity<String> resp = get("/api/v1/patient/profile", token);
        assertEquals(403, resp.getStatusCode().value());
        assertEquals(403, body(resp).get("code").asInt());
    }

    @Test
    @DisplayName("DOCTOR 访问 /api/v1/patient/** 默认 HTTP 403")
    void doctorForbiddenOnPatientEndpoints() throws Exception {
        String token = login("doctor01", "doctor123");
        ResponseEntity<String> resp = get("/api/v1/patient/records", token);
        assertEquals(403, resp.getStatusCode().value());
    }

    // ---------- 附加：无 Token 访问患者接口 → 401 ----------

    @Test
    @DisplayName("无 Token 访问 /api/v1/patient/profile 返回 HTTP 401")
    void noTokenReturns401() throws Exception {
        ResponseEntity<String> resp = get("/api/v1/patient/profile", null);
        assertEquals(401, resp.getStatusCode().value());
        assertEquals(401, body(resp).get("code").asInt());
    }
}
