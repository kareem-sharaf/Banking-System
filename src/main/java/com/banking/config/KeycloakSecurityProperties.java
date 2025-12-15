package com.banking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak Security Properties
 * Maps Keycloak configuration from application.properties
 * Used for token requests and client configuration
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakSecurityProperties {

    private String realm;
    private String authServerUrl;
    private String clientId;
    private String clientSecret;
    private String tokenUri;

    // Admin Configuration (optional - for programmatic Keycloak management)
    @Data
    @Configuration
    @ConfigurationProperties(prefix = "keycloak.admin")
    public static class AdminProperties {
        private String realm;
        private String serverUrl;
        private String clientId;
        private String username;
        private String password;
    }
}
