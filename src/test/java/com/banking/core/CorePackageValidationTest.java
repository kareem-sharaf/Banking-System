package com.banking.core;

import com.banking.core.auth.module.adapter.AuthenticationProvider;
import com.banking.core.auth.module.adapter.imp.KeycloakAuthenticationAdapter;
import com.banking.core.config.*;
import com.banking.core.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive validation test for the core package
 * Tests configuration, security setup, and component wiring
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "keycloak.auth-server-url=http://localhost:8180",
    "keycloak.realm=banking-system",
    "keycloak.client-id=banking-client",
    "keycloak.client-secret=test-secret"
})
public class CorePackageValidationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private KeycloakSecurityProperties keycloakProperties;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    /**
     * Test that all core configurations are properly loaded
     */
    @Test
    void testCoreConfigurationsLoaded() {
        // Test Security Configuration
        SecurityConfig securityConfig = applicationContext.getBean(SecurityConfig.class);
        assertThat(securityConfig).isNotNull();

        // Test Authentication Provider Configuration
        AuthenticationProviderConfig authConfig = applicationContext.getBean(AuthenticationProviderConfig.class);
        assertThat(authConfig).isNotNull();

        // Test Keycloak Properties
        assertThat(keycloakProperties).isNotNull();
        assertThat(keycloakProperties.getRealm()).isEqualTo("banking-system");
        assertThat(keycloakProperties.getAuthServerUrl()).isEqualTo("http://localhost:8180");
        assertThat(keycloakProperties.getClientId()).isEqualTo("banking-client");
    }

    /**
     * Test that authentication provider is properly configured
     */
    @Test
    void testAuthenticationProviderConfiguration() {
        // Authentication provider should be available (may be null if Keycloak is down)
        if (authenticationProvider != null) {
            assertThat(authenticationProvider).isInstanceOf(KeycloakAuthenticationAdapter.class);
            assertThat(authenticationProvider.getProviderName()).isEqualTo("Keycloak");
        }
    }

    /**
     * Test that Keycloak adapter bean exists
     */
    @Test
    void testKeycloakAdapterBean() {
        KeycloakAuthenticationAdapter keycloakAdapter =
            applicationContext.getBean(KeycloakAuthenticationAdapter.class);
        assertThat(keycloakAdapter).isNotNull();
        assertThat(keycloakAdapter.getProviderName()).isEqualTo("Keycloak");
    }

    /**
     * Test that global exception handler is configured
     */
    @Test
    void testGlobalExceptionHandler() {
        assertThat(globalExceptionHandler).isNotNull();
    }

    /**
     * Test that all required services are available
     */
    @Test
    void testRequiredServicesAvailable() {
        // Test that approval config is available
        ApprovalConfig approvalConfig = applicationContext.getBean(ApprovalConfig.class);
        assertThat(approvalConfig).isNotNull();

        // Test that data seeder is available
        DataSeeder dataSeeder = applicationContext.getBean(DataSeeder.class);
        assertThat(dataSeeder).isNotNull();

        // Test that scheduler config is available
        SchedulerConfig schedulerConfig = applicationContext.getBean(SchedulerConfig.class);
        assertThat(schedulerConfig).isNotNull();
    }

    /**
     * Test that all enums are properly defined
     */
    @Test
    void testEnumsDefined() {
        // Test that all core enums exist and have values
        assertThat(com.banking.core.enums.AccountState.values()).isNotEmpty();
        assertThat(com.banking.core.enums.TransactionStatus.values()).isNotEmpty();
        assertThat(com.banking.core.enums.ApprovalStatus.values()).isNotEmpty();
        assertThat(com.banking.core.enums.NotificationPriority.values()).isNotEmpty();
    }

    /**
     * Test that security filter chains are properly configured
     */
    @Test
    void testSecurityFilterChains() throws Exception {
        SecurityConfig securityConfig = applicationContext.getBean(SecurityConfig.class);

        // Test that CORS configuration is available
        var corsConfig = securityConfig.corsConfigurationSource();
        assertThat(corsConfig).isNotNull();

        // Test basic CORS configuration properties by creating a mock request
        var corsConfiguration = corsConfig.getCorsConfiguration(
            new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/test"));
        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOrigins())
            .contains("http://localhost:3000", "http://localhost:8080");
        assertThat(corsConfiguration.getAllowedMethods())
            .contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    /**
     * Test that all core controllers are properly configured
     */
    @Test
    void testControllersConfigured() {
        // Test that notification controller exists
        var notificationController = applicationContext.getBean(
            com.banking.core.notification.controller.NotificationController.class);
        assertThat(notificationController).isNotNull();

        // Test that audit log controller exists (even if empty)
        var auditController = applicationContext.getBean(
            com.banking.core.controller.AuditLogController.class);
        assertThat(auditController).isNotNull();
    }
}
