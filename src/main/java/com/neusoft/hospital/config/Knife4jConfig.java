package com.neusoft.hospital.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("东软智慧云脑诊疗平台 API")
                        .description("覆盖科室/挂号/检查/检验/处方/处置/患者门户/账号管理等全部业务模块。"
                                + "JWT v2 认证（ver=2 + role + tv），三角色平级 ADMIN/DOCTOR/PATIENT。")
                        .version("v2.0.0")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@neusoft.edu.cn")))
                .components(new Components()
                        .addSecuritySchemes("Bearer",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT v2 Token（login 获取，Bearer 前缀拼接）")));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("1-认证管理")
                .displayName("认证管理（登录/登出/改密/患者注册）")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi accountApi() {
        return GroupedOpenApi.builder()
                .group("2-账号管理")
                .displayName("账号管理（仅 ADMIN）")
                .pathsToMatch("/api/v1/account/**")
                .build();
    }

    @Bean
    public GroupedOpenApi dictApi() {
        return GroupedOpenApi.builder()
                .group("3-字典管理")
                .displayName("字典管理（科室/号别/结算类别/排班 仅ADMIN；药品/疾病/医技 ADMIN+DOCTOR）")
                .pathsToMatch("/api/v1/department/**",
                        "/api/v1/regist-level/**",
                        "/api/v1/settle-category/**",
                        "/api/v1/scheduling/**",
                        "/api/v1/medical-technology/**",
                        "/api/v1/disease/**",
                        "/api/v1/drug-info/**")
                .build();
    }

    @Bean
    public GroupedOpenApi employeeApi() {
        return GroupedOpenApi.builder()
                .group("4-员工管理")
                .displayName("员工管理（仅 ADMIN）")
                .pathsToMatch("/api/v1/employee/**")
                .build();
    }

    @Bean
    public GroupedOpenApi clinicalApi() {
        return GroupedOpenApi.builder()
                .group("5-诊疗业务")
                .displayName("诊疗业务（ADMIN + DOCTOR；DOCTOR 仅本人接诊范围）")
                .pathsToMatch("/api/v1/register/**",
                        "/api/v1/check-request/**",
                        "/api/v1/inspection-request/**",
                        "/api/v1/disposal-request/**",
                        "/api/v1/prescription/**")
                .build();
    }

    @Bean
    public GroupedOpenApi patientApi() {
        return GroupedOpenApi.builder()
                .group("6-患者门户")
                .displayName("患者门户（仅 PATIENT）")
                .pathsToMatch("/api/v1/patient/**")
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("0-全部接口")
                .displayName("全部接口")
                .pathsToMatch("/api/**")
                .build();
    }
}
