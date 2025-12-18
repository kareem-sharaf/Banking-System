package com.banking.core.auth.adapter;

import com.banking.core.auth.adapter.dto.*;

/**
 * Common interface for authentication providers
 * This interface abstracts authentication operations from specific implementations
 * (Keycloak, Auth0, Okta, etc.)
 * 
 * Implements the Adapter Pattern to allow switching between different
 * authentication providers without changing business logic.
 */
public interface AuthenticationProvider {

    /**
     * Authenticate a user with username and password
     * 
     * @param username The username
     * @param password The password
     * @return TokenResponse containing access token and refresh token
     */
    TokenResponse authenticate(String username, String password);

    /**
     * Refresh an access token using a refresh token
     * 
     * @param refreshToken The refresh token
     * @return TokenResponse containing new access token and refresh token
     */
    TokenResponse refreshToken(String refreshToken);

    /**
     * Get user information from an access token
     * 
     * @param accessToken The access token
     * @return UserInfo containing user details
     */
    UserInfo getUserInfo(String accessToken);

    /**
     * Validate if a token is valid
     * 
     * @param token The token to validate
     * @return true if token is valid, false otherwise
     */
    boolean validateToken(String token);

    /**
     * Create a new user
     * 
     * @param request User registration request
     * @return UserRegistrationResult containing user ID and status
     */
    UserRegistrationResult createUser(UserRegistrationRequest request);

    /**
     * Update an existing user
     * 
     * @param userId The user ID
     * @param request User update request
     * @return true if update was successful, false otherwise
     */
    boolean updateUser(String userId, UserUpdateRequest request);

    /**
     * Check if a username exists
     * 
     * @param username The username to check
     * @return true if username exists, false otherwise
     */
    boolean usernameExists(String username);

    /**
     * Assign a role to a user
     * 
     * @param userId The user ID
     * @param roleName The role name
     * @return true if role was assigned successfully, false otherwise
     */
    boolean assignRoleToUser(String userId, String roleName);

    /**
     * Remove a role from a user
     * 
     * @param userId The user ID
     * @param roleName The role name
     * @return true if role was removed successfully, false otherwise
     */
    boolean removeRoleFromUser(String userId, String roleName);

    /**
     * Logout a user (invalidate refresh token)
     * 
     * @param refreshToken The refresh token to invalidate
     * @return true if logout was successful, false otherwise
     */
    boolean logout(String refreshToken);

    /**
     * Get the provider name (e.g., "Keycloak", "Auth0", "Okta")
     * 
     * @return The provider name
     */
    String getProviderName();
}
