package com.neusoft.hospital.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neusoft.hospital.auth.context.AuthUser;
import com.neusoft.hospital.auth.context.CurrentUser;
import com.neusoft.hospital.auth.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ThreadLocal 泄漏回归测试。
 * <p>
 * 使用 MOCK web 环境 + MockMvc：拦截器在测试线程上同步执行，因此请求返回后
 * 可直接在同一线程读取 {@link CurrentUser}，确定性验证 401/403/异常路径不残留 AuthUser。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ThreadLocalLeakRegressionTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    StringRedisTemplate redis;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void resetState() throws Exception {
        try (Connection conn = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(conn,
                    new EncodedResource(new ClassPathResource("sql/rbac_fixture.sql"), StandardCharsets.UTF_8));
        }
        redis.execute((org.springframework.data.redis.core.RedisCallback<Object>) c -> {
            c.flushDb();
            return null;
        });
        CurrentUser.clear();
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(body);
        assertEquals(200, node.get("code").asInt());
        return node.get("data").get("token").asText();
    }

    @Test
    @DisplayName("DOCTOR 访问 @RequireRole(ADMIN) 得 403，且 ThreadLocal 不残留 AuthUser")
    void doctorForbiddenLeavesNoThreadLocal() throws Exception {
        String token = login("doctor01", "doctor123");
        assertNull(CurrentUser.getAuthUser(), "用例前置：测试线程应干净");

        mockMvc.perform(get("/api/v1/test/admin").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        assertNull(CurrentUser.getAuthUser(),
                "403 路径不得在 ThreadLocal 残留 AuthUser（set 必须在 @RequireRole 之后）");
    }

    @Test
    @DisplayName("@Public 请求开始时会清空预先写入的伪造 CurrentUser")
    void publicPathClearsStaleThreadLocal() throws Exception {
        // 预先手工写入伪造身份
        CurrentUser.set(AuthUser.builder().accountId(999).role(Role.DOCTOR).build());
        assertNotNull(CurrentUser.getAuthUser(), "前置：已写入伪造 AuthUser");

        // 调用 @Public 路径，断言 Controller 内读到的是清空后的 ANONYMOUS
        mockMvc.perform(get("/api/v1/test/whoami-public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ANONYMOUS"));

        assertNull(CurrentUser.getAuthUser(), "@Public 请求结束后 ThreadLocal 应为空");
    }

    @Test
    @DisplayName("ADMIN 正常访问成功，且 Controller 内能读取到正确 AuthUser")
    void adminSuccessControllerReadsCorrectAuthUser() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(get("/api/v1/test/whoami").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("ADMIN:1"));

        assertNull(CurrentUser.getAuthUser(),
                "成功请求由 afterCompletion 清理后 ThreadLocal 应为空");
    }

    @Test
    @DisplayName("401（无 Token）路径不残留 AuthUser")
    void unauthorizedLeavesNoThreadLocal() throws Exception {
        assertNull(CurrentUser.getAuthUser());
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        assertNull(CurrentUser.getAuthUser(), "401 路径不得残留 AuthUser");
    }
}
