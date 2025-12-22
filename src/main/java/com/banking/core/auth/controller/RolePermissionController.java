package com.banking.core.auth.controller;

import com.banking.core.auth.dto.CreateRolePermissionRequest;
import com.banking.core.auth.dto.RolePermissionResponse;
import com.banking.core.auth.service.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Role Permission Controller
 *
 * REST API for Role Permission Management (CRUD).
 *
 * @author Banking System
 */
@RestController
@RequestMapping("/api/role-permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private static final Logger logger = LoggerFactory.getLogger(RolePermissionController.class);

    private final RolePermissionService rolePermissionService;

    /**
     * Create a new role permission
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RolePermissionResponse> createRolePermission(@Valid @RequestBody CreateRolePermissionRequest request) {
        logger.info("Received request to create role permission: roleId={}, permissionId={}",
                   request.getRoleId(), request.getPermissionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(rolePermissionService.createRolePermission(request));
    }

    /**
     * Get all role permissions
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RolePermissionResponse>> getAllRolePermissions() {
        return ResponseEntity.ok(rolePermissionService.getAllRolePermissions());
    }

    /**
     * Get role permission by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RolePermissionResponse> getRolePermissionById(@PathVariable Long id) {
        return ResponseEntity.ok(rolePermissionService.getRolePermissionById(id));
    }

    /**
     * Get role permissions by role ID
     */
    @GetMapping("/role/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RolePermissionResponse>> getRolePermissionsByRoleId(@PathVariable Long roleId) {
        return ResponseEntity.ok(rolePermissionService.getRolePermissionsByRoleId(roleId));
    }

    /**
     * Get role permissions by permission ID
     */
    @GetMapping("/permission/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RolePermissionResponse>> getRolePermissionsByPermissionId(@PathVariable Long permissionId) {
        return ResponseEntity.ok(rolePermissionService.getRolePermissionsByPermissionId(permissionId));
    }

    /**
     * Delete role permission by ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRolePermission(@PathVariable Long id) {
        rolePermissionService.deleteRolePermission(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete role permission by role and permission IDs
     */
    @DeleteMapping("/role/{roleId}/permission/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRolePermissionByRoleAndPermission(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        rolePermissionService.deleteRolePermissionByRoleAndPermission(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if role permission exists
     */
    @GetMapping("/exists/role/{roleId}/permission/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> existsByRoleIdAndPermissionId(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        return ResponseEntity.ok(rolePermissionService.existsByRoleIdAndPermissionId(roleId, permissionId));
    }
}
