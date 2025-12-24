package com.banking.core.config;

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
 * OpenAPI Configuration for Swagger Documentation
 *
 * This configuration provides comprehensive API documentation
 * with security schemes and server configurations.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI bankingSystemOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking System API")
                        .description("Comprehensive Banking System with Microservices Architecture")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Banking System Team")
                                .email("support@banking-system.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development Server"),
                        new Server()
                                .url("https://banking-system.onrender.com")
                                .description("Production Server")
                ))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\""))
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Basic authentication for admin endpoints")))
                .security(List.of(
                        new SecurityRequirement().addList("bearerAuth"),
                        new SecurityRequirement().addList("basicAuth")
                ));
    }
}
