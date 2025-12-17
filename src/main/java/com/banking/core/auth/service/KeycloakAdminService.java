package com.banking.core.auth.service;

import com.banking.config.KeycloakSecurityProperties;
import com.banking.core.auth.dto.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing Keycloak users via Admin API
 */
@Service
public class KeycloakAdminService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);

    @Autowired
    private KeycloakSecurityProperties keycloakProperties;

    @Value("${keycloak.admin.server-url}")
    private String adminServerUrl;

    @Value("${keycloak.admin.realm}")
    private String adminRealm;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Get admin access token
     */
    private String getAdminAccessToken() {
        try {
            String tokenUrl = adminServerUrl + "/realms/" + adminRealm + "/protocol/openid-connect/token";

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("grant_type", "password");
            requestBody.add("client_id", "admin-cli");
            requestBody.add("username", adminUsername);
            requestBody.add("password", adminPassword);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate
                    .postForEntity(tokenUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            logger.error("Error getting admin access token", e);
        }
        return null;
    }

    /**
     * Create realm if it doesn't exist
     */
    public boolean ensureRealmExists() {
        try {
            String adminToken = getAdminAccessToken();
            if (adminToken == null) {
                logger.error("Cannot create realm: Failed to get admin access token");
                return false;
            }

            String realm = keycloakProperties.getRealm();

            // Check if realm exists
            String realmUrl = adminServerUrl + "/admin/realms/" + realm;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> checkRequest = new HttpEntity<>(headers);
            try {
                restTemplate.exchange(realmUrl, HttpMethod.GET, checkRequest, Map.class);
                logger.info("Realm {} already exists", realm);
                return true; // Realm exists
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    // Realm doesn't exist, create it
                    logger.info("Realm {} not found, creating it...", realm);
                    return createRealm(adminToken, realm);
                }
                throw e;
            }
        } catch (Exception e) {
            logger.error("Error ensuring realm exists", e);
            return false;
        }
    }

    /**
     * Create a new realm in Keycloak
     */
    private boolean createRealm(String adminToken, String realmName) {
        try {
            String realmsUrl = adminServerUrl + "/admin/realms";

            Map<String, Object> realmRepresentation = new HashMap<>();
            realmRepresentation.put("realm", realmName);
            realmRepresentation.put("enabled", true);
            realmRepresentation.put("displayName", "Banking System");
            realmRepresentation.put("loginWithEmailAllowed", true);
            realmRepresentation.put("duplicateEmailsAllowed", false);
            realmRepresentation.put("resetPasswordAllowed", true);
            realmRepresentation.put("editUsernameAllowed", false);
            realmRepresentation.put("bruteForceProtected", true);
            realmRepresentation.put("permanentLockout", false);
            realmRepresentation.put("maxFailureWaitSeconds", 900);
            realmRepresentation.put("minimumQuickLoginWaitSeconds", 60);
            realmRepresentation.put("waitIncrementSeconds", 60);
            realmRepresentation.put("quickLoginCheckMilliSeconds", 1000);
            realmRepresentation.put("maxDeltaTimeSeconds", 43200);
            realmRepresentation.put("failureFactor", 30);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(realmRepresentation, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    realmsUrl,
                    HttpMethod.POST,
                    request,
                    Void.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                logger.info("Realm {} created successfully", realmName);

                // Create default roles
                createDefaultRoles(adminToken, realmName);

                // Create client
                createDefaultClient(adminToken, realmName);

                return true;
            }
        } catch (Exception e) {
            logger.error("Error creating realm {}", realmName, e);
        }
        return false;
    }

    /**
     * Create default roles in realm
     */
    private void createDefaultRoles(String adminToken, String realmName) {
        String[] roles = { "CUSTOMER", "TELLER", "MANAGER", "ADMIN" };

        for (String roleName : roles) {
            try {
                String rolesUrl = adminServerUrl + "/admin/realms/" + realmName + "/roles";

                Map<String, Object> role = new HashMap<>();
                role.put("name", roleName);
                role.put("description", roleName + " role for banking system");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(adminToken);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(role, headers);
                restTemplate.postForEntity(rolesUrl, request, Void.class);
                logger.info("Role {} created in realm {}", roleName, realmName);
            } catch (Exception e) {
                logger.warn("Error creating role {}: {}", roleName, e.getMessage());
            }
        }
    }

    /**
     * Create default client in realm
     */
    private void createDefaultClient(String adminToken, String realmName) {
        try {
            String clientsUrl = adminServerUrl + "/admin/realms/" + realmName + "/clients";

            Map<String, Object> client = new HashMap<>();
            client.put("clientId", keycloakProperties.getClientId());
            client.put("enabled", true);
            client.put("clientAuthenticatorType", "client-secret");
            client.put("standardFlowEnabled", true);
            client.put("directAccessGrantsEnabled", true);
            client.put("publicClient", false);
            client.put("redirectUris", List.of("http://localhost:8080/*"));
            client.put("webOrigins", List.of("*"));

            // Generate client secret
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(client, headers);
            ResponseEntity<Void> response = restTemplate.postForEntity(clientsUrl, request, Void.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                logger.info("Client {} created in realm {}", keycloakProperties.getClientId(), realmName);
                // Note: Client secret will be generated by Keycloak, need to retrieve it
                // separately
            }
        } catch (Exception e) {
            logger.warn("Error creating client: {}", e.getMessage());
        }
    }

    /**
     * Create a new user in Keycloak
     */
    public Map<String, Object> createUser(RegisterRequest registerRequest) {
        try {
            // Ensure realm exists before creating user
            if (!ensureRealmExists()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Failed to create or access realm. Please check Keycloak configuration.");
                return error;
            }

            String adminToken = getAdminAccessToken();
            if (adminToken == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Failed to get admin access token. Please check Keycloak admin credentials.");
                return error;
            }

            String realm = keycloakProperties.getRealm();
            logger.info("Attempting to create user in realm: {}", realm);
            String usersUrl = adminServerUrl + "/admin/realms/" + realm + "/users";

            // Prepare user representation
            Map<String, Object> userRepresentation = new HashMap<>();
            userRepresentation.put("username", registerRequest.getUsername());
            userRepresentation.put("email", registerRequest.getEmail());
            userRepresentation.put("firstName", registerRequest.getFirstName());
            userRepresentation.put("lastName", registerRequest.getLastName());
            userRepresentation.put("enabled", true);
            userRepresentation.put("emailVerified", true);

            // Set credentials
            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", registerRequest.getPassword());
            credential.put("temporary", false);

            List<Map<String, Object>> credentials = new ArrayList<>();
            credentials.add(credential);
            userRepresentation.put("credentials", credentials);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(userRepresentation, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    usersUrl,
                    HttpMethod.POST,
                    request,
                    Void.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                // Get the created user ID from Location header
                String location = response.getHeaders().getFirst("Location");
                String userId = location.substring(location.lastIndexOf('/') + 1);

                // Ensure role exists before assigning
                String roleToAssign = registerRequest.getRole() != null ? registerRequest.getRole() : "USER";
                ensureRoleExists(roleToAssign);

                // Assign role to user
                assignRoleToUser(userId, roleToAssign, adminToken);

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "User created successfully");
                result.put("userId", userId);
                return result;
            }

        } catch (HttpClientErrorException e) {
            logger.error("Error creating user in Keycloak: Status={}, Body={}", e.getStatusCode(),
                    e.getResponseBodyAsString(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                error.put("message", "Username or email already exists");
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                String realm = keycloakProperties.getRealm();
                error.put("message", "Realm '" + realm + "' not found in Keycloak. Please create the realm first.");
                error.put("realm", realm);
                error.put("help", "Go to Keycloak Admin Console and create realm: " + realm);
            } else {
                error.put("message", "Failed to create user: " + e.getMessage());
                error.put("details", e.getResponseBodyAsString());
            }
            return error;
        } catch (Exception e) {
            logger.error("Error creating user in Keycloak", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error creating user: " + e.getMessage());
            error.put("exception", e.getClass().getSimpleName());
            return error;
        }

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Failed to create user");
        return error;
    }

    /**
     * Assign role to user (public method)
     */
    public boolean assignRoleToUser(String userId, String roleName) {
        try {
            String adminToken = getAdminAccessToken();
            if (adminToken == null) {
                logger.error("Failed to get admin access token");
                return false;
            }
            return assignRoleToUser(userId, roleName, adminToken);
        } catch (Exception e) {
            logger.error("Error assigning role to user", e);
            return false;
        }
    }

    /**
     * Assign role to user (private method with token)
     */
    private boolean assignRoleToUser(String userId, String roleName, String adminToken) {
        try {
            String realm = keycloakProperties.getRealm();

            // Get role representation
            String rolesUrl = adminServerUrl + "/admin/realms/" + realm + "/roles/" + roleName;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> getRequest = new HttpEntity<>(headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> roleResponse = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate
                    .exchange(
                            rolesUrl,
                            HttpMethod.GET,
                            getRequest,
                            Map.class);

            if (roleResponse.getStatusCode() == HttpStatus.OK && roleResponse.getBody() != null) {
                Map<String, Object> roleRepresentation = roleResponse.getBody();

                // Assign role to user
                String userRolesUrl = adminServerUrl + "/admin/realms/" + realm + "/users/" + userId
                        + "/role-mappings/realm";
                List<Map<String, Object>> rolesList = new ArrayList<>();
                rolesList.add(roleRepresentation);
                HttpEntity<List<Map<String, Object>>> assignRequest = new HttpEntity<>(rolesList, headers);

                ResponseEntity<Void> assignResponse = restTemplate.exchange(
                        userRolesUrl,
                        HttpMethod.POST,
                        assignRequest,
                        Void.class);

                if (assignResponse.getStatusCode() == HttpStatus.NO_CONTENT ||
                        assignResponse.getStatusCode() == HttpStatus.OK) {
                    logger.info("Role {} assigned to user {}", roleName, userId);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error assigning role to user", e);
        }
        return false;
    }

    /**
     * Check if username exists
     */
    public boolean usernameExists(String username) {
        try {
            String adminToken = getAdminAccessToken();
            if (adminToken == null) {
                return false;
            }

            String realm = keycloakProperties.getRealm();
            String usersUrl = adminServerUrl + "/admin/realms/" + realm + "/users?username=" + username;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<List<Map<String, Object>>> response = (ResponseEntity<List<Map<String, Object>>>) (ResponseEntity<?>) restTemplate
                    .exchange(
                            usersUrl,
                            HttpMethod.GET,
                            request,
                            List.class);

            return response.getBody() != null && !response.getBody().isEmpty();
        } catch (Exception e) {
            logger.error("Error checking if username exists", e);
            return false;
        }
    }

    /**
     * Validate token with Keycloak
     */
    public boolean validateToken(String token) {
        try {
            String realm = keycloakProperties.getRealm();
            String introspectUrl = adminServerUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("token", token);
            body.add("client_id", keycloakProperties.getClientId());
            body.add("client_secret", keycloakProperties.getClientSecret());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate
                    .postForEntity(introspectUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Boolean active = (Boolean) response.getBody().get("active");
                return active != null && active;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error validating token", e);
            return false;
        }
    }

    /**
     * Get user info from token
     */
    public Map<String, Object> getUserInfoFromToken(String token) {
        try {
            String realm = keycloakProperties.getRealm();
            String userInfoUrl = adminServerUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate
                    .exchange(
                            userInfoUrl,
                            HttpMethod.GET,
                            request,
                            Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            return new HashMap<>();
        } catch (Exception e) {
            logger.error("Error getting user info from token", e);
            return new HashMap<>();
        }
    }

    /**
     * Logout from Keycloak
     */
    public boolean logout(String refreshToken) {
        try {
            String realm = keycloakProperties.getRealm();
            String logoutUrl = adminServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("refresh_token", refreshToken);
            body.add("client_id", keycloakProperties.getClientId());
            body.add("client_secret", keycloakProperties.getClientSecret());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Void> response = restTemplate.postForEntity(logoutUrl, request, Void.class);

            return response.getStatusCode() == HttpStatus.NO_CONTENT || response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            logger.error("Error logging out from Keycloak", e);
            return false;
        }
    }

    /**
     * Get user ID from Keycloak by username
     */
    public String getUserIdByUsername(String username) {
        try {
            String adminToken = getAdminAccessToken();
            if (adminToken == null) {
                return null;
            }

            String realm = keycloakProperties.getRealm();
            String usersUrl = adminServerUrl + "/admin/realms/" + realm + "/users?username=" + username;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<List<Map<String, Object>>> response = (ResponseEntity<List<Map<String, Object>>>) (ResponseEntity<?>) restTemplate
                    .exchange(
                            usersUrl,
                            HttpMethod.GET,
                            request,
                            List.class);

            if (response.getBody() != null && !response.getBody().isEmpty()) {
                Map<String, Object> user = response.getBody().get(0);
                return (String) user.get("id");
            }
            return null;
        } catch (Exception e) {
            logger.error("Error getting user ID by username", e);
            return null;
        }
    }

    /**
     * Create role in Keycloak realm
     */
    public boolean createRole(String roleName, String description) {
        try {
            String adminToken = getAdminAccessToken();
            if (adminToken == null) {
                logger.error("Failed to get admin access token");
                return false;
            }

            String realm = keycloakProperties.getRealm();
            String rolesUrl = adminServerUrl + "/admin/realms/" + realm + "/roles";

            Map<String, Object> role = new HashMap<>();
            role.put("name", roleName);
            if (description != null) {
                role.put("description", description);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(role, headers);

            try {
                ResponseEntity<Void> response = restTemplate.postForEntity(rolesUrl, request, Void.class);
                if (response.getStatusCode() == HttpStatus.CREATED) {
                    logger.info("Role {} created successfully", roleName);
                    return true;
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.CONFLICT) {
                    logger.info("Role {} already exists", roleName);
                    return true; // Role already exists, consider it success
                }
                throw e;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error creating role {}", roleName, e);
            return false;
        }
    }

    /**
     * Get all roles from Keycloak realm
     */
    public List<Map<String, Object>> getAllRoles() {
        try {
            String adminToken = getAdminAccessToken();
            if (adminToken == null) {
                return new ArrayList<>();
            }

            String realm = keycloakProperties.getRealm();
            String rolesUrl = adminServerUrl + "/admin/realms/" + realm + "/roles";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<List<Map<String, Object>>> response = (ResponseEntity<List<Map<String, Object>>>) (ResponseEntity<?>) restTemplate
                    .exchange(
                            rolesUrl,
                            HttpMethod.GET,
                            request,
                            List.class);

            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error getting all roles", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get user roles from Keycloak
     */
    public List<Map<String, Object>> getUserRoles(String userId) {
        try {
            String adminToken = getAdminAccessToken();
            if (adminToken == null) {
                return new ArrayList<>();
            }

            String realm = keycloakProperties.getRealm();
            String userRolesUrl = adminServerUrl + "/admin/realms/" + realm + "/users/" + userId
                    + "/role-mappings/realm";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate
                    .exchange(
                            userRolesUrl,
                            HttpMethod.GET,
                            request,
                            Map.class);

            if (response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> roles = (List<Map<String, Object>>) response.getBody().get("roles");
                return roles != null ? roles : new ArrayList<>();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error getting user roles", e);
            return new ArrayList<>();
        }
    }

    /**
     * Remove role from user
     */
    public boolean removeRoleFromUser(String userId, String roleName) {
        try {
            String adminToken = getAdminAccessToken();
            if (adminToken == null) {
                logger.error("Failed to get admin access token");
                return false;
            }

            String realm = keycloakProperties.getRealm();

            // Get role representation
            String rolesUrl = adminServerUrl + "/admin/realms/" + realm + "/roles/" + roleName;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> getRequest = new HttpEntity<>(headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> roleResponse = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate
                    .exchange(
                            rolesUrl,
                            HttpMethod.GET,
                            getRequest,
                            Map.class);

            if (roleResponse.getStatusCode() == HttpStatus.OK && roleResponse.getBody() != null) {
                Map<String, Object> roleRepresentation = roleResponse.getBody();

                // Remove role from user
                String userRolesUrl = adminServerUrl + "/admin/realms/" + realm + "/users/" + userId
                        + "/role-mappings/realm";
                List<Map<String, Object>> rolesList = new ArrayList<>();
                rolesList.add(roleRepresentation);
                HttpEntity<List<Map<String, Object>>> removeRequest = new HttpEntity<>(rolesList, headers);

                ResponseEntity<Void> removeResponse = restTemplate.exchange(
                        userRolesUrl,
                        HttpMethod.DELETE,
                        removeRequest,
                        Void.class);

                if (removeResponse.getStatusCode() == HttpStatus.NO_CONTENT) {
                    logger.info("Role {} removed from user {}", roleName, userId);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Error removing role from user", e);
            return false;
        }
    }

    /**
     * Ensure role exists, create if not
     */
    public boolean ensureRoleExists(String roleName) {
        try {
            String adminToken = getAdminAccessToken();
            if (adminToken == null) {
                return false;
            }

            String realm = keycloakProperties.getRealm();
            String rolesUrl = adminServerUrl + "/admin/realms/" + realm + "/roles/" + roleName;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<Void> checkRequest = new HttpEntity<>(headers);
            try {
                restTemplate.exchange(rolesUrl, HttpMethod.GET, checkRequest, Map.class);
                logger.info("Role {} already exists", roleName);
                return true; // Role exists
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    // Role doesn't exist, create it
                    logger.info("Role {} not found, creating it...", roleName);
                    return createRole(roleName, roleName + " role for banking system");
                }
                throw e;
            }
        } catch (Exception e) {
            logger.error("Error ensuring role exists", e);
            return false;
        }
    }
}
