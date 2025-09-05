package com.freshmarket.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI文档配置
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8080}")
    private int serverPort;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fresh Market API")
                        .version("1.0.0-SNAPSHOT")
                        .description("生鲜电商系统 RESTful API 文档")
                        .contact(new Contact()
                                .name("Fresh Market Team")
                                .email("contact@freshmarket.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort + "/api/v1")
                                .description("本地开发环境"),
                        new Server()
                                .url("https://api.freshmarket.com/api/v1")
                                .description("生产环境")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT认证令牌")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}