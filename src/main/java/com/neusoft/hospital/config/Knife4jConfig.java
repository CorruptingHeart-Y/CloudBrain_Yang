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
                        .description("东软智慧云脑诊疗平台后端接口文档，涵盖科室、挂号、检查检验、处方等全部业务模块")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@neusoft.edu.cn")))
                .components(new Components()
                        .addSecuritySchemes("Bearer",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Token 认证")));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("认证管理")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi dictApi() {
        return GroupedOpenApi.builder()
                .group("字典管理")
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
    public GroupedOpenApi businessApi() {
        return GroupedOpenApi.builder()
                .group("业务管理")
                .pathsToMatch("/api/v1/employee/**",
                        "/api/v1/register/**",
                        "/api/v1/check-request/**",
                        "/api/v1/inspection-request/**",
                        "/api/v1/disposal-request/**",
                        "/api/v1/prescription/**")
                .build();
    }

    @Bean
    public GroupedOpenApi aiApi() {
        return GroupedOpenApi.builder()
                .group("AI诊前分诊")
                .pathsToMatch("/api/v1/triage/**")
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("全部接口")
                .pathsToMatch("/api/**")
                .build();
    }
}
