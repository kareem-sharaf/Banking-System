package com.banking.core.auth.controller;

import com.banking.core.auth.dto.AuthResponse;
import com.banking.core.auth.dto.LoginRequest;
import com.banking.core.auth.dto.RegisterRequest;
import com.banking.core.auth.module.entity.User;
import com.banking.core.auth.service.KeycloakAdminService;
import com.banking.core.auth.service.KeycloakTokenService;
import com.banking.core.auth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Authentication Controller - Endpoints for user registration and login via
 * Keycloak
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakTokenService keycloakTokenService;
    private final UserService userService;

    public AuthController(KeycloakAdminService keycloakAdminService,
            KeycloakTokenService keycloakTokenService,
            UserService userService) {
        this.keycloakAdminService = keycloakAdminService;
        this.keycloakTokenService = keycloakTokenService;
        this.userService = userService;
    }

    /**
     * Get information about the authenticated user
     * Requires: Any authenticated user
     */
    @GetMapping("/user-info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getUserInfo(Principal principal) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", principal.getName());
        userInfo.put("authenticated", true);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtToken.getToken();

            List<String> authorities = authentication.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .collect(Collectors.toList());

            userInfo.put("authorities", authorities);
            userInfo.put("email", jwt.getClaimAsString("email"));
            userInfo.put("givenName", jwt.getClaimAsString("given_name"));
            userInfo.put("familyName", jwt.getClaimAsString("family_name"));
            userInfo.put("preferredUsername", jwt.getClaimAsString("preferred_username"));
            userInfo.put("subject", jwt.getSubject());

            // Extract roles from token
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                userInfo.put("realmAccess", realmAccess);
            }
        }

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Get current user details
     * Requires: Any authenticated user with role CUSTOMER, TELLER, MANAGER, or
     * ADMIN
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> userData = new HashMap<>();

        userData.put("principal", authentication.getName());
        userData.put("authorities", authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList()));
        userData.put("authenticated", authentication.isAuthenticated());

        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtToken.getToken();
            userData.put("email", jwt.getClaimAsString("email"));
            userData.put("preferredUsername", jwt.getClaimAsString("preferred_username"));
        }

        return ResponseEntity.ok(userData);
    }

    /**
     * Register a new user
     * Public endpoint - no authentication required
     * 
     * Workflow:
     * 1. Validate input data
     * 2. Check if username/email already exists (Keycloak + Local DB)
     * 3. Create user in Keycloak via Admin REST API
     * - Ensure realm exists
     * - Create user with credentials
     * - Ensure role exists (create if not)
     * - Assign role to user
     * 4. Save user data in local database
     * - Link with Keycloak ID
     * - Set local role
     * 5. Record registration in LoginHistory
     * - Save IP address, User Agent, timestamp
     * 6. Get access token from Keycloak
     * 7. Return token + user info (user can use token immediately)
     * 
     * Response includes:
     * - accessToken: JWT token for authentication
     * - refreshToken: Token to refresh access token
     * - expiresIn: Token expiration time
     * - userInfo: User details from local database
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest,
            HttpServletRequest request) {
        // Check if username already exists in Keycloak
        if (keycloakAdminService.usernameExists(registerRequest.getUsername())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Username already exists");
            error.put("message", "The username '" + registerRequest.getUsername() + "' is already taken");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        // Check if username or email exists in local database
        if (userService.findByUsername(registerRequest.getUsername()).isPresent()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Username already exists");
            error.put("message", "The username '" + registerRequest.getUsername() + "' is already taken");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        // Create user in Keycloak
        Map<String, Object> createResult = keycloakAdminService.createUser(registerRequest);

        if ((Boolean) createResult.getOrDefault("success", false)) {
            String keycloakId = (String) createResult.get("userId");

            // Save user in local database
            User localUser = userService.createLocalUser(registerRequest, keycloakId);

            if (localUser != null) {
                // Record registration in LoginHistory
                String ipAddress = userService.getClientIpAddress(request);
                String userAgent = userService.getUserAgent(request);
                userService.updateLoginHistory(localUser, ipAddress, userAgent, true, null);

                // Get token from Keycloak after successful registration
                Map<String, Object> tokenResponse = keycloakTokenService.getAccessToken(
                        registerRequest.getUsername(),
                        registerRequest.getPassword());

                if (tokenResponse.containsKey("access_token")) {
                    // Success - return token + user info
                    AuthResponse authResponse = new AuthResponse();
                    authResponse.setAccessToken((String) tokenResponse.get("access_token"));
                    authResponse.setRefreshToken((String) tokenResponse.get("refresh_token"));
                    authResponse.setExpiresIn(((Number) tokenResponse.get("expires_in")).longValue());
                    authResponse.setTokenType("Bearer");
                    authResponse.setScope((String) tokenResponse.get("scope"));
                    authResponse.setMessage("User registered and logged in successfully");

                    // Add user info
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("id", localUser.getId());
                    userData.put("username", localUser.getUsername());
                    userData.put("email", localUser.getEmail());
                    userData.put("firstName", localUser.getFirstName());
                    userData.put("lastName", localUser.getLastName());
                    userData.put("keycloakId", localUser.getKeycloakId());
                    authResponse.setUserInfo(userData);

                    return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
                } else {
                    // User created but token retrieval failed
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "User registered successfully but failed to get token. Please login.");
                    response.put("userId", localUser.getId());
                    response.put("username", localUser.getUsername());
                    response.put("email", localUser.getEmail());
                    response.put("warning", "Token retrieval failed: "
                            + tokenResponse.getOrDefault("error_description", "Unknown error"));
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                }
            } else {
                // Keycloak user created but local user creation failed
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message",
                        "User created in Keycloak but failed to save locally. Please contact administrator.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createResult);
        }
    }

    /**
     * Login endpoint
     * Public endpoint - no authentication required
     * Workflow:
     * 1. Get token from Keycloak
     * 2. Validate token
     * 3. Update lastLoginAt for user
     * 4. Record login in LoginHistory
     * 5. Return token + basic user info
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        Map<String, Object> tokenResponse = keycloakTokenService.getAccessToken(
                loginRequest.getUsername(),
                loginRequest.getPassword());

        if (tokenResponse.containsKey("access_token")) {
            String accessToken = (String) tokenResponse.get("access_token");

            // Get user info from token
            Map<String, Object> userInfo = keycloakAdminService.getUserInfoFromToken(accessToken);
            String keycloakId = (String) userInfo.get("sub");

            // Find or create local user
            Optional<User> userOpt = userService.findByKeycloakId(keycloakId);
            User user;

            if (userOpt.isPresent()) {
                user = userOpt.get();
                // Update last login time
                userService.updateLastLoginAt(user);
            } else {
                // User exists in Keycloak but not in local DB - try to find by username
                userOpt = userService.findByUsername(loginRequest.getUsername());
                if (userOpt.isPresent()) {
                    user = userOpt.get();
                    user.setKeycloakId(keycloakId);
                    userService.updateLastLoginAt(user);
                } else {
                    // User not found locally - this shouldn't happen if registration worked
                    user = null;
                }
            }

            // Record login in LoginHistory
            if (user != null) {
                String ipAddress = userService.getClientIpAddress(request);
                String userAgent = userService.getUserAgent(request);
                userService.updateLoginHistory(user, ipAddress, userAgent, true, null);
            }

            AuthResponse authResponse = new AuthResponse();
            authResponse.setAccessToken(accessToken);
            authResponse.setRefreshToken((String) tokenResponse.get("refresh_token"));
            authResponse.setExpiresIn(((Number) tokenResponse.get("expires_in")).longValue());
            authResponse.setTokenType("Bearer");
            authResponse.setScope((String) tokenResponse.get("scope"));
            authResponse.setMessage("Login successful");

            // Add basic user info
            Map<String, Object> userData = new HashMap<>();
            if (user != null) {
                userData.put("id", user.getId());
                userData.put("username", user.getUsername());
                userData.put("email", user.getEmail());
                userData.put("firstName", user.getFirstName());
                userData.put("lastName", user.getLastName());
            } else {
                userData.put("username", userInfo.get("preferred_username"));
                userData.put("email", userInfo.get("email"));
            }
            authResponse.setUserInfo(userData);

            return ResponseEntity.ok(authResponse);
        } else {
            // Record failed login attempt
            Optional<User> userOpt = userService.findByUsername(loginRequest.getUsername());
            if (userOpt.isPresent()) {
                String ipAddress = userService.getClientIpAddress(request);
                String userAgent = userService.getUserAgent(request);
                String failureReason = tokenResponse.getOrDefault("error_description", "Invalid credentials")
                        .toString();
                userService.updateLoginHistory(userOpt.get(), ipAddress, userAgent, false, failureReason);
            }

            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid credentials");
            error.put("message",
                    tokenResponse.getOrDefault("error_description", "Invalid username or password").toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Refresh access token
     * Public endpoint - no authentication required
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refresh_token");

        if (refreshToken == null || refreshToken.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "refresh_token is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Map<String, Object> tokenResponse = keycloakTokenService.refreshToken(refreshToken);

        if (tokenResponse.containsKey("access_token")) {
            AuthResponse authResponse = new AuthResponse();
            authResponse.setAccessToken((String) tokenResponse.get("access_token"));
            authResponse.setRefreshToken((String) tokenResponse.get("refresh_token"));
            authResponse.setExpiresIn(((Number) tokenResponse.get("expires_in")).longValue());
            authResponse.setTokenType("Bearer");
            authResponse.setMessage("Token refreshed successfully");

            return ResponseEntity.ok(authResponse);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(tokenResponse);
        }
    }

    /**
     * Validate token endpoint
     * Public endpoint - no authentication required (token is in header)
     * Workflow:
     * 1. Validate token with Keycloak
     * 2. Return user data if token is valid
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Missing or invalid Authorization header");
            error.put("message", "Authorization header must be in format: Bearer {token}");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        String token = authHeader.substring(7);

        // Validate token with Keycloak
        boolean isValid = keycloakAdminService.validateToken(token);

        if (isValid) {
            // Get user info from token
            Map<String, Object> userInfo = keycloakAdminService.getUserInfoFromToken(token);
            String keycloakId = (String) userInfo.get("sub");

            // Get local user data
            Optional<User> userOpt = userService.findByKeycloakId(keycloakId);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("message", "Token is valid");

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("username", user.getUsername());
                userData.put("email", user.getEmail());
                userData.put("firstName", user.getFirstName());
                userData.put("lastName", user.getLastName());
                userData.put("active", user.isActive());
                response.put("user", userData);
            } else {
                // User exists in Keycloak but not in local DB
                response.put("user", userInfo);
                response.put("warning", "User not found in local database");
            }

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("valid", false);
            error.put("error", "Invalid or expired token");
            error.put("message", "The provided token is invalid or has expired");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Logout endpoint
     * Requires authentication
     * Workflow:
     * 1. Logout from Keycloak
     * 2. Record logout time in LoginHistory
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout(@RequestBody(required = false) Map<String, String> request,
            HttpServletRequest httpRequest) {
        String refreshToken = null;
        if (request != null) {
            refreshToken = request.get("refresh_token");
        }

        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String keycloakId = null;

        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtToken.getToken();
            keycloakId = jwt.getSubject();
        }

        // Logout from Keycloak if refresh token provided
        boolean keycloakLogoutSuccess = true;
        if (refreshToken != null && !refreshToken.isEmpty()) {
            keycloakLogoutSuccess = keycloakAdminService.logout(refreshToken);
        }

        // Update logout time in local database
        if (keycloakId != null) {
            Optional<User> userOpt = userService.findByKeycloakId(keycloakId);
            if (userOpt.isPresent()) {
                userService.updateLogoutHistory(userOpt.get());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logged out successfully");
        if (!keycloakLogoutSuccess && refreshToken != null) {
            response.put("warning", "Keycloak logout may have failed, but local logout recorded");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint - requires authentication
     */
    @GetMapping("/test")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> testAuth() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Authentication successful!");
        response.put("status", "You are authenticated");
        return ResponseEntity.ok(response);
    }
}
