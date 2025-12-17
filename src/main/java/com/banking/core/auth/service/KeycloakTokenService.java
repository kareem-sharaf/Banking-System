package com.banking.core.auth.service;

import com.banking.config.KeycloakSecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for obtaining access tokens from Keycloak
 * This service can be used for service-to-service authentication
 */
@Service
@RequiredArgsConstructor
public class KeycloakTokenService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakTokenService.class);
    private final KeycloakSecurityProperties keycloakProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get access token using username and password (Resource Owner Password
     * Credentials Grant)
     */
    public Map<String, Object> getAccessToken(String username, String password) {
        String tokenUri = keycloakProperties.getTokenUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", keycloakProperties.getClientId());
        body.add("client_secret", keycloakProperties.getClientSecret());
        body.add("username", username);
        body.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate
                    .postForEntity(tokenUri, request, Map.class);
            return response.getBody() != null ? response.getBody() : new HashMap<>();
        } catch (HttpClientErrorException e) {
            logger.error("Keycloak token request failed: {}", e.getResponseBodyAsString());
            Map<String, Object> error = new HashMap<>();

            // Try to parse the error response from Keycloak
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> errorResponse = objectMapper.readValue(e.getResponseBodyAsString(), Map.class);
                error.put("error", errorResponse.getOrDefault("error", "invalid_grant"));
                error.put("error_description",
                        errorResponse.getOrDefault("error_description", "Invalid username or password"));
                error.put("message", errorResponse.getOrDefault("error_description", "Invalid username or password"));
            } catch (Exception parseException) {
                // If parsing fails, use default error message
                error.put("error", "invalid_grant");
                error.put("error_description", "Invalid username or password");
                error.put("message", "Invalid username or password");
            }
            return error;
        } catch (Exception e) {
            logger.error("Unexpected error during token request", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "internal_error");
            error.put("error_description", "Failed to obtain token: " + e.getMessage());
            error.put("message", "Failed to obtain token: " + e.getMessage());
            return error;
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public Map<String, Object> refreshToken(String refreshToken) {
        String tokenUri = keycloakProperties.getTokenUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", keycloakProperties.getClientId());
        body.add("client_secret", keycloakProperties.getClientSecret());
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate
                    .postForEntity(tokenUri, request, Map.class);
            return response.getBody() != null ? response.getBody() : new HashMap<>();
        } catch (HttpClientErrorException e) {
            logger.error("Keycloak token refresh failed: {}", e.getResponseBodyAsString());
            Map<String, Object> error = new HashMap<>();

            // Try to parse the error response from Keycloak
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> errorResponse = objectMapper.readValue(e.getResponseBodyAsString(), Map.class);
                error.put("error", errorResponse.getOrDefault("error", "invalid_grant"));
                error.put("error_description",
                        errorResponse.getOrDefault("error_description", "Invalid refresh token"));
                error.put("message", errorResponse.getOrDefault("error_description", "Invalid refresh token"));
            } catch (Exception parseException) {
                // If parsing fails, use default error message
                error.put("error", "invalid_grant");
                error.put("error_description", "Invalid refresh token");
                error.put("message", "Invalid refresh token");
            }
            return error;
        } catch (Exception e) {
            logger.error("Unexpected error during token refresh", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "internal_error");
            error.put("error_description", "Failed to refresh token: " + e.getMessage());
            error.put("message", "Failed to refresh token: " + e.getMessage());
            return error;
        }
    }
}
