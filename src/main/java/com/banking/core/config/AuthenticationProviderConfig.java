package com.banking.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.banking.core.auth.module.adapter.AuthenticationProvider;
import com.banking.core.auth.module.adapter.imp.KeycloakAuthenticationAdapter;

/**
 * Configuration for Authentication Provider
 * 
 * This configuration explicitly defines which AuthenticationProvider implementation
 * to use. Currently configured to use Keycloak, but can be easily switched to
 * another provider (Auth0, Okta, etc.) by changing the bean definition.
 * 
 * To switch providers:
 * 1. Create a new adapter implementing AuthenticationProvider (e.g., Auth0AuthenticationAdapter)
 * 2. Change the @Bean method below to return the new adapter
 * 3. Or use @Primary annotation on the desired adapter class
 */
@Configuration
@Slf4j
public class AuthenticationProviderConfig {

    /**
     * Primary authentication provider bean
     * Currently configured to use Keycloak
     */
    @Bean
    @Primary
    public AuthenticationProvider authenticationProvider(KeycloakAuthenticationAdapter keycloakAdapter) {
        log.info("Configuring AuthenticationProvider: {}", keycloakAdapter.getProviderName());
        return keycloakAdapter;
    }
}
