package com.banking.core.auth.service;

import com.banking.core.auth.dto.CreateRolePermissionRequest;
import com.banking.core.auth.dto.RolePermissionResponse;
import com.banking.core.auth.module.entity.Permission;
import com.banking.core.auth.module.entity.Role;
import com.banking.core.auth.module.entity.RolePermission;
import com.banking.core.auth.repository.PermissionRepository;
import com.banking.core.auth.repository.RolePermissionRepository;
import com.banking.core.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Role Permission Service
 *
 * Service layer for Role Permission Management (CRUD).
 *
 * @author Banking System
 */
@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private static final Logger logger = LoggerFactory.getLogger(RolePermissionService.class);

    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Create a new role permission
     */
    @Transactional
    public RolePermissionResponse createRolePermission(CreateRolePermissionRequest request) {
        logger.info("Creating role permission: roleId={}, permissionId={}",
                   request.getRoleId(), request.getPermissionId());

        // Validate role exists
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + request.getRoleId()));

        // Validate permission exists
        Permission permission = permissionRepository.findById(request.getPermissionId())
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with ID: " + request.getPermissionId()));

        // Check if role permission already exists
        if (rolePermissionRepository.existsByRoleIdAndPermissionId(request.getRoleId(), request.getPermissionId())) {
            throw new IllegalArgumentException("Role permission already exists for role '" +
                                             role.getName() + "' and permission '" + permission.getName() + "'");
        }

        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);

        RolePermission savedRolePermission = rolePermissionRepository.save(rolePermission);
        logger.info("Created role permission with ID: {}", savedRolePermission.getId());

        return mapToRolePermissionResponse(savedRolePermission);
    }

    /**
     * Get all role permissions
     */
    public List<RolePermissionResponse> getAllRolePermissions() {
        logger.info("Retrieving all role permissions");
        return rolePermissionRepository.findAll().stream()
                .map(this::mapToRolePermissionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get role permission by ID
     */
    public RolePermissionResponse getRolePermissionById(Long id) {
        logger.info("Retrieving role permission with ID: {}", id);
        RolePermission rolePermission = rolePermissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role permission not found with ID: " + id));
        return mapToRolePermissionResponse(rolePermission);
    }

    /**
     * Get role permissions by role ID
     */
    public List<RolePermissionResponse> getRolePermissionsByRoleId(Long roleId) {
        logger.info("Retrieving role permissions for role ID: {}", roleId);
        return rolePermissionRepository.findByRoleId(roleId).stream()
                .map(this::mapToRolePermissionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get role permissions by permission ID
     */
    public List<RolePermissionResponse> getRolePermissionsByPermissionId(Long permissionId) {
        logger.info("Retrieving role permissions for permission ID: {}", permissionId);
        return rolePermissionRepository.findByPermissionId(permissionId).stream()
                .map(this::mapToRolePermissionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete role permission
     */
    @Transactional
    public void deleteRolePermission(Long id) {
        logger.info("Deleting role permission with ID: {}", id);

        if (!rolePermissionRepository.existsById(id)) {
            throw new IllegalArgumentException("Role permission not found with ID: " + id);
        }

        rolePermissionRepository.deleteById(id);
        logger.info("Deleted role permission with ID: {}", id);
    }

    /**
     * Delete role permission by role and permission IDs
     */
    @Transactional
    public void deleteRolePermissionByRoleAndPermission(Long roleId, Long permissionId) {
        logger.info("Deleting role permission: roleId={}, permissionId={}", roleId, permissionId);

        RolePermission rolePermission = rolePermissionRepository
                .findByRoleIdAndPermissionId(roleId, permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Role permission not found for role ID " +
                                                              roleId + " and permission ID " + permissionId));

        rolePermissionRepository.delete(rolePermission);
        logger.info("Deleted role permission with ID: {}", rolePermission.getId());
    }

    /**
     * Check if role permission exists
     */
    public boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId) {
        return rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId);
    }

    private RolePermissionResponse mapToRolePermissionResponse(RolePermission rolePermission) {
        return RolePermissionResponse.builder()
                .id(rolePermission.getId())
                .roleId(rolePermission.getRole().getId())
                .roleName(rolePermission.getRole().getName())
                .permissionId(rolePermission.getPermission().getId())
                .permissionName(rolePermission.getPermission().getName())
                .permissionResource(rolePermission.getPermission().getResource())
                .permissionAction(rolePermission.getPermission().getAction())
                .build();
    }
}
