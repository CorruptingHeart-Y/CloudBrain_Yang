package com.neusoft.hospital.identity;

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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

/**
 * v2.0 身份 schema 与三角色 /register 行为端到端验证（本地隔离库，绝不连远程）。
 * <p>
 * 验证：零 ALTER 既有业务表、新表结构、PATIENT /register 只读兼容与隔离、DOCTOR/ADMIN 范围。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class V2IdentityIntegrationTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.data.redis.core.StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void resetState() throws Exception {
        rest.getRestTemplate().setRequestFactory(new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
        try (Connection conn = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(conn, new EncodedResource(new ClassPathResource("sql/rbac_fixture.sql"), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(conn, new EncodedResource(new ClassPathResource("sql/pr3_patient_fixture.sql"), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(conn, new EncodedResource(new ClassPathResource("sql/pr4_doctor_fixture.sql"), StandardCharsets.UTF_8));
        }
        redis.execute((org.springframework.data.redis.core.RedisCallback<Object>) c -> { c.flushDb(); return null; });
    }

    private String url(String p) { return "http://localhost:" + port + p; }
    private String login(String u, String p) throws Exception {
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> r = rest.postForEntity(url("/api/v1/auth/login"),
                new HttpEntity<>(mapper.writeValueAsString(Map.of("username", u, "password", p)), h), String.class);
        assertEquals(200, r.getStatusCode().value(), "登录应200: " + r.getBody());
        return mapper.readTree(r.getBody()).get("data").get("token").asText();
    }
    private ResponseEntity<String> get(String path, String token) throws Exception {
        HttpHeaders h = new HttpHeaders(); if (token != null) h.set("Authorization", "Bearer " + token);
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(h), String.class);
    }
    private ResponseEntity<String> post(String path, String token, Object body) throws Exception {
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.set("Authorization", "Bearer " + token);
        return rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(mapper.writeValueAsString(body), h), String.class);
    }

    // ---------- 1-4. 零 ALTER 既有业务表 + 新表结构 ----------

    @Test
    @DisplayName("register 无 patient_id 列；employee 无 role / token_version 列（零 ALTER）")
    void legacyTablesNotAltered() {
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='register' AND column_name='patient_id'", Integer.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='employee' AND column_name='role'", Integer.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='employee' AND column_name='token_version'", Integer.class));
    }

    @Test
    @DisplayName("v2.0 新表 user_account/patient/patient_register_link 存在且约束齐全")
    void newTablesAndConstraints() {
        for (String t : new String[]{"user_account", "patient", "patient_register_link"}) {
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='" + t + "'", Integer.class),
                    "表 " + t + " 应存在");
        }
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_account' AND column_name='token_version'", Integer.class));
        assertTrue(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='user_account' AND index_name='uk_employee_id'", Integer.class) >= 1,
                "uk_employee_id 应存在");
        assertTrue(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='user_account' AND index_name='uk_patient_id'", Integer.class) >= 1,
                "uk_patient_id 应存在");
        assertTrue(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='patient_register_link' AND index_name='uk_register_id'", Integer.class) >= 1,
                "link.uk_register_id 应存在");
        assertTrue(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='patient_register_link' AND index_name='uk_patient_register'", Integer.class) >= 1,
                "link.uk_patient_register 应存在");
    }

    // ---------- 5. PATIENT /register 只读兼容 ----------

    @Test
    @DisplayName("PATIENT GET /register 仅返回 link 到本人的记录；GET /register/{他人} → 404")
    void patientRegisterReadScoped() throws Exception {
        String t = login("patient01", "patient123");
        ResponseEntity<String> resp = get("/api/v1/register", t);
        assertEquals(200, resp.getStatusCode().value());
        JsonNode data = mapper.readTree(resp.getBody()).get("data");
        Set<Integer> ids = StreamSupport.stream(data.get("records").spliterator(), false)
                .map(n -> n.get("id").asInt()).collect(Collectors.toSet());
        assertTrue(ids.contains(1) && ids.contains(2), "patientA 应见 link 的 1,2");
        assertFalse(ids.contains(3), "不得见 patientB 的 register 3");
        assertFalse(ids.contains(10), "不得见医生B的 register 10");
        // 本人 link 的 register 1 → 200；他人 register 3 → 404
        assertEquals(200, get("/api/v1/register/1", t).getStatusCode().value());
        assertEquals(404, get("/api/v1/register/3", t).getStatusCode().value());
    }

    @Test
    @DisplayName("PATIENT POST /register 写操作 → 403")
    void patientRegisterWriteForbidden() throws Exception {
        String t = login("patient01", "patient123");
        ResponseEntity<String> r = post("/api/v1/register", t, Map.of(
                "caseNumber", "BL_X", "realName", "张三", "gender", "男",
                "deptmentId", 1, "registLevelId", 1, "settleCategoryId", 1));
        assertEquals(403, r.getStatusCode().value());
    }

    // ---------- 6-10. DOCTOR / ADMIN /register 范围 ----------

    @Test
    @DisplayName("DOCTOR GET /register 仅返回本人接诊；他人 register → 404")
    void doctorRegisterScoped() throws Exception {
        String t = login("doctor01", "doctor123");
        JsonNode data = mapper.readTree(get("/api/v1/register", t).getBody()).get("data");
        Set<Integer> ids = StreamSupport.stream(data.get("records").spliterator(), false)
                .map(n -> n.get("id").asInt()).collect(Collectors.toSet());
        assertTrue(ids.contains(1), "doctorA 应见本人接诊的 1");
        assertFalse(ids.contains(10), "不得见医生B的 register 10");
        assertEquals(404, get("/api/v1/register/10", t).getStatusCode().value());
    }

    @Test
    @DisplayName("ADMIN GET /register 跨医生可见 1 与 10")
    void adminRegisterCrossDoctor() throws Exception {
        String t = login("admin", "admin123");
        assertEquals(200, get("/api/v1/register/1", t).getStatusCode().value());
        assertEquals(200, get("/api/v1/register/10", t).getStatusCode().value());
    }

    // ---------- 11-12. 挂号创建加固：case_number 服务端生成 / regist_money 按级别推导 / visit_state=1 ----------

    @Test
    @DisplayName("ADMIN 建号：服务端生成 case_number、regist_money 取级别费用、visit_state=1")
    void adminCreateRegisterGeneratesCaseNumberAndFee() throws Exception {
        String t = login("admin", "admin123");
        ResponseEntity<String> r = post("/api/v1/register", t, Map.of(
                "realName", "测试新增", "gender", "男",
                "deptmentId", 1, "registLevelId", 1, "settleCategoryId", 1));
        assertEquals(200, r.getStatusCode().value(), "建号应200: " + r.getBody());
        JsonNode data = mapper.readTree(r.getBody()).get("data");
        assertNotNull(data.get("id"), "应回填 id");
        String caseNumber = data.get("caseNumber").asText();
        assertTrue(caseNumber.matches("BL\\d{16}"), "case_number 应为 BL+16位数字，实际: " + caseNumber);
        assertEquals(10.00, data.get("registMoney").asDouble(), "regist_money 应取 regist_level.fee=10.00");
        assertEquals(1, data.get("visitState").asInt(), "建号即 visit_state=1");
        assertEquals("普通号", data.get("registLevelName").asText(), "应回填级别名称");
    }

    @Test
    @DisplayName("建号 registLevelId 不存在 → 真实 404")
    void createRegisterRejectsUnknownRegistLevel() throws Exception {
        String t = login("admin", "admin123");
        ResponseEntity<String> r = post("/api/v1/register", t, Map.of(
                "realName", "测试", "gender", "男",
                "deptmentId", 1, "registLevelId", 999, "settleCategoryId", 1));
        assertEquals(404, r.getStatusCode().value(), "未知挂号级别应 404: " + r.getBody());
    }
}
