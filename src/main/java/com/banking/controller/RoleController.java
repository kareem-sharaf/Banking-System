package com.banking.controller;

import com.banking.service.KeycloakAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing roles in Keycloak
 * All operations are done via REST API without accessing Keycloak Admin Console
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final KeycloakAdminService keycloakAdminService;

    public RoleController(KeycloakAdminService keycloakAdminService) {
        this.keycloakAdminService = keycloakAdminService;
    }

    /**
     * Get all roles from Keycloak
     * Requires: ADMIN role
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllRoles() {
        List<Map<String, Object>> roles = keycloakAdminService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * Create a new role in Keycloak
     * Requires: ADMIN role
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRole(@RequestBody Map<String, String> request) {
        String roleName = request.get("name");
        String description = request.get("description");

        if (roleName == null || roleName.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Role name is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        boolean success = keycloakAdminService.createRole(roleName, description);
        if (success) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Role created successfully");
            response.put("roleName", roleName);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create role");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Assign role to user
     * Requires: ADMIN role
     */
    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignRoleToUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String roleName = request.get("roleName");

        if (username == null || username.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Username is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        if (roleName == null || roleName.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Role name is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        // Ensure role exists
        if (!keycloakAdminService.ensureRoleExists(roleName)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to ensure role exists");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        // Get user ID from Keycloak
        String userId = keycloakAdminService.getUserIdByUsername(username);
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found in Keycloak");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        // Assign role
        boolean success = keycloakAdminService.assignRoleToUser(userId, roleName);
        if (success) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Role assigned successfully");
            response.put("username", username);
            response.put("roleName", roleName);
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to assign role");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Remove role from user
     * Requires: ADMIN role
     */
    @PostMapping("/remove")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeRoleFromUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String roleName = request.get("roleName");

        if (username == null || username.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Username is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        if (roleName == null || roleName.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Role name is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        // Get user ID from Keycloak
        String userId = keycloakAdminService.getUserIdByUsername(username);
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found in Keycloak");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        // Remove role
        boolean success = keycloakAdminService.removeRoleFromUser(userId, roleName);
        if (success) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Role removed successfully");
            response.put("username", username);
            response.put("roleName", roleName);
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to remove role");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get user roles
     * Requires: ADMIN role or the user themselves
     */
    @GetMapping("/user/{username}")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #username")
    public ResponseEntity<?> getUserRoles(@PathVariable String username) {
        // Get user ID from Keycloak
        String userId = keycloakAdminService.getUserIdByUsername(username);
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found in Keycloak");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        List<Map<String, Object>> roles = keycloakAdminService.getUserRoles(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("roles", roles);
        return ResponseEntity.ok(response);
    }

    /**
     * Ensure default roles exist
     * Requires: ADMIN role
     */
    @PostMapping("/ensure-defaults")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> ensureDefaultRoles() {
        String[] defaultRoles = { "USER", "ADMIN", "CUSTOMER", "TELLER", "MANAGER" };
        Map<String, Object> response = new HashMap<>();
        Map<String, Boolean> results = new HashMap<>();

        for (String roleName : defaultRoles) {
            boolean success = keycloakAdminService.ensureRoleExists(roleName);
            results.put(roleName, success);
        }

        response.put("success", true);
        response.put("message", "Default roles ensured");
        response.put("results", results);
        return ResponseEntity.ok(response);
    }
}
