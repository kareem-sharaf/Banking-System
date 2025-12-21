package com.banking.core.auth.module.adapter.imp;

import com.banking.core.auth.dto.RegisterRequest;
import com.banking.core.auth.dto.TokenResponse;
import com.banking.core.auth.dto.UserInfo;
import com.banking.core.auth.dto.UserRegistrationRequest;
import com.banking.core.auth.dto.UserRegistrationResult;
import com.banking.core.auth.dto.UserUpdateRequest;
import com.banking.core.auth.module.adapter.AuthenticationProvider;
import com.banking.core.auth.service.KeycloakAdminService;
import com.banking.core.auth.service.KeycloakTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Keycloak Authentication Adapter
 * 
 * Adapts Keycloak-specific services (KeycloakAdminService, KeycloakTokenService)
 * to the common AuthenticationProvider interface.
 * 
 * This implements the Adapter Pattern, allowing the system to switch between
 * different authentication providers without changing business logic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthenticationAdapter implements AuthenticationProvider {

    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakTokenService keycloakTokenService;

    @Override
    public TokenResponse authenticate(String username, String password) {
        try {
            Map<String, Object> keycloakResponse = keycloakTokenService.getAccessToken(username, password);
            return adaptToTokenResponse(keycloakResponse);
        } catch (Exception e) {
            log.error("Error authenticating user: {}", username, e);
            return TokenResponse.builder()
                    .success(false)
                    .error("authentication_failed")
                    .errorDescription("Failed to authenticate user: " + e.getMessage())
                    .message("Authentication failed")
                    .build();
        }
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        try {
            Map<String, Object> keycloakResponse = keycloakTokenService.refreshToken(refreshToken);
            return adaptToTokenResponse(keycloakResponse);
        } catch (Exception e) {
            log.error("Error refreshing token", e);
            return TokenResponse.builder()
                    .success(false)
                    .error("token_refresh_failed")
                    .errorDescription("Failed to refresh token: " + e.getMessage())
                    .message("Token refresh failed")
                    .build();
        }
    }

    @Override
    public UserInfo getUserInfo(String accessToken) {
        try {
            Map<String, Object> keycloakUserInfo = keycloakAdminService.getUserInfoFromToken(accessToken);
            return adaptToUserInfo(keycloakUserInfo);
        } catch (Exception e) {
            log.error("Error getting user info from token", e);
            return UserInfo.builder()
                    .enabled(false)
                    .build();
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            return keycloakAdminService.validateToken(token);
        } catch (Exception e) {
            log.error("Error validating token", e);
            return false;
        }
    }

    @Override
    public UserRegistrationResult createUser(UserRegistrationRequest request) {
        try {
            // Convert UserRegistrationRequest to RegisterRequest for KeycloakAdminService
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setUsername(request.getUsername());
            registerRequest.setEmail(request.getEmail());
            registerRequest.setPassword(request.getPassword());
            registerRequest.setFirstName(request.getFirstName());
            registerRequest.setLastName(request.getLastName());
            registerRequest.setPhoneNumber(request.getPhoneNumber());
            registerRequest.setRole(request.getRole() != null ? request.getRole() : "CUSTOMER");

            Map<String, Object> keycloakResult = keycloakAdminService.createUser(registerRequest);
            return adaptToUserRegistrationResult(keycloakResult);
        } catch (Exception e) {
            log.error("Error creating user: {}", request.getUsername(), e);
            return UserRegistrationResult.builder()
                    .success(false)
                    .error("user_creation_failed")
                    .errorDescription("Failed to create user: " + e.getMessage())
                    .message("User creation failed")
                    .build();
        }
    }

    @Override
    public boolean updateUser(String userId, UserUpdateRequest request) {
        // KeycloakAdminService doesn't have a direct updateUser method
        // This would need to be implemented in KeycloakAdminService or handled differently
        log.warn("updateUser not yet implemented in KeycloakAdminService");
        return false;
    }

    @Override
    public boolean usernameExists(String username) {
        try {
            return keycloakAdminService.usernameExists(username);
        } catch (Exception e) {
            log.error("Error checking if username exists: {}", username, e);
            return false;
        }
    }

    @Override
    public boolean assignRoleToUser(String userId, String roleName) {
        try {
            return keycloakAdminService.assignRoleToUser(userId, roleName);
        } catch (Exception e) {
            log.error("Error assigning role {} to user {}", roleName, userId, e);
            return false;
        }
    }

    @Override
    public boolean removeRoleFromUser(String userId, String roleName) {
        try {
            return keycloakAdminService.removeRoleFromUser(userId, roleName);
        } catch (Exception e) {
            log.error("Error removing role {} from user {}", roleName, userId, e);
            return false;
        }
    }

    @Override
    public boolean logout(String refreshToken) {
        try {
            return keycloakAdminService.logout(refreshToken);
        } catch (Exception e) {
            log.error("Error logging out user", e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "Keycloak";
    }

    /**
     * Adapt Keycloak token response to common TokenResponse DTO
     */
    private TokenResponse adaptToTokenResponse(Map<String, Object> keycloakResponse) {
        if (keycloakResponse.containsKey("error")) {
            return TokenResponse.builder()
                    .success(false)
                    .error((String) keycloakResponse.get("error"))
                    .errorDescription((String) keycloakResponse.getOrDefault("error_description", ""))
                    .message((String) keycloakResponse.getOrDefault("message", ""))
                    .build();
        }

        String accessToken = (String) keycloakResponse.get("access_token");
        String refreshToken = (String) keycloakResponse.get("refresh_token");
        String tokenType = (String) keycloakResponse.getOrDefault("token_type", "Bearer");
        Integer expiresIn = (Integer) keycloakResponse.get("expires_in");
        String scope = (String) keycloakResponse.get("scope");

        Instant expiresAt = expiresIn != null ? Instant.now().plusSeconds(expiresIn) : null;

        return TokenResponse.builder()
                .success(true)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(tokenType)
                .expiresIn(expiresIn != null ? expiresIn.longValue() : null)
                .expiresAt(expiresAt)
                .scope(scope)
                .build();
    }

    /**
     * Adapt Keycloak user info to common UserInfo DTO
     */
    private UserInfo adaptToUserInfo(Map<String, Object> keycloakUserInfo) {
        if (keycloakUserInfo == null || keycloakUserInfo.isEmpty()) {
            return UserInfo.builder().enabled(false).build();
        }

        // Extract roles from realm_access if available
        List<String> roles = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = (Map<String, Object>) keycloakUserInfo.get("realm_access");
        if (realmAccess != null) {
            @SuppressWarnings("unchecked")
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            if (realmRoles != null) {
                roles.addAll(realmRoles);
            }
        }

        return UserInfo.builder()
                .userId((String) keycloakUserInfo.get("sub"))
                .username((String) keycloakUserInfo.getOrDefault("preferred_username", keycloakUserInfo.get("username")))
                .email((String) keycloakUserInfo.get("email"))
                .firstName((String) keycloakUserInfo.get("given_name"))
                .lastName((String) keycloakUserInfo.get("family_name"))
                .emailVerified((Boolean) keycloakUserInfo.getOrDefault("email_verified", false))
                .roles(roles)
                .attributes(keycloakUserInfo)
                .enabled(true)
                .build();
    }

    /**
     * Adapt Keycloak user creation result to common UserRegistrationResult DTO
     */
    private UserRegistrationResult adaptToUserRegistrationResult(Map<String, Object> keycloakResult) {
        Boolean success = (Boolean) keycloakResult.getOrDefault("success", false);
        String userId = (String) keycloakResult.get("userId");
        String message = (String) keycloakResult.get("message");

        if (success && userId != null) {
            return UserRegistrationResult.builder()
                    .success(true)
                    .userId(userId)
                    .message(message != null ? message : "User created successfully")
                    .build();
        } else {
            return UserRegistrationResult.builder()
                    .success(false)
                    .error("user_creation_failed")
                    .errorDescription(message != null ? message : "Failed to create user")
                    .message(message)
                    .build();
        }
    }
}
