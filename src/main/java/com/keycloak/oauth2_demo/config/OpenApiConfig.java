package com.keycloak.oauth2_demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerJWT";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OAuth2 Keycloak & JWT Authentication API")
                        .version("1.0.0")
                        .description("Tài liệu Swagger UI tập trung cho 3 API cốt lõi: Đăng nhập (Login), Đăng ký (Register) và Lấy Profile (Get Profile).")
                        .contact(new Contact()
                                .name("OAuth2 Keycloak Team")
                                .email("support@example.com"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhập JWT Access Token nhận được từ API Đăng nhập")));
    }

    /**
     * Chỉ gom nhóm và hiển thị 3 REST API (/api/**) trên Swagger UI, ẩn toàn bộ HTML views
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("core-apis")
                .pathsToMatch("/api/**")
                .build();
    }
}
