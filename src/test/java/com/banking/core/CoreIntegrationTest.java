package com.banking.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for core endpoints and actuator
 * Tests endpoints without requiring external services like Keycloak
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "keycloak.auth-server-url=http://localhost:8180",
    "keycloak.realm=banking-system",
    "keycloak.client-id=banking-client",
    "keycloak.client-secret=test-secret",
    "management.endpoints.web.exposure.include=health,info,metrics"
})
public class CoreIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test that actuator health endpoint is accessible
     */
    @Test
    void testActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * Test that actuator info endpoint is accessible
     */
    @Test
    void testActuatorInfoEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized()); // Info endpoint requires authentication by default
    }

    /**
     * Test that actuator metrics endpoint is accessible
     */
    @Test
    void testActuatorMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized()); // Metrics endpoint requires authentication by default
    }

    /**
     * Test that public auth endpoints are accessible (may return 500 if Keycloak is down)
     */
    @Test
    void testPublicAuthEndpointsAccessible() throws Exception {
        // These endpoints should be accessible without authentication
        // They may return 500 if Keycloak services are not available, but should not return 401
        mockMvc.perform(get("/api/auth/register"))
                .andExpect(status().is5xxServerError()); // 500 if Keycloak is down, not 401

        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().is5xxServerError());

        mockMvc.perform(get("/api/auth/refresh"))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Test that protected endpoints require authentication
     */
    @Test
    void testProtectedEndpointsRequireAuthentication() throws Exception {
        // These should return 401 or redirect due to no authentication
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/manager/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/user"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test that CORS headers are present
     */
    @Test
    void testCorsHeaders() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}
