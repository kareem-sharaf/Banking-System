package com.banking.core.auth.service;

import com.banking.core.auth.dto.CreatePermissionRequest;
import com.banking.core.auth.dto.PermissionResponse;
import com.banking.core.auth.dto.UpdatePermissionRequest;
import com.banking.core.auth.module.entity.Permission;
import com.banking.core.auth.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Permission Service
 *
 * Service layer for Permission Management (CRUD).
 *
 * @author Banking System
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionService.class);

    private final PermissionRepository permissionRepository;

    /**
     * Create a new permission
     */
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        logger.info("Creating new permission: {}", request.getName());

        // Check if permission name already exists
        if (permissionRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Permission with name '" + request.getName() + "' already exists");
        }

        Permission permission = new Permission();
        permission.setName(request.getName());
        permission.setResource(request.getResource());
        permission.setAction(request.getAction());
        permission.setDescription(request.getDescription());

        Permission savedPermission = permissionRepository.save(permission);
        logger.info("Created permission with ID: {}", savedPermission.getId());

        return mapToPermissionResponse(savedPermission);
    }

    /**
     * Get all permissions
     */
    public List<PermissionResponse> getAllPermissions() {
        logger.info("Retrieving all permissions");
        return permissionRepository.findAll().stream()
                .map(this::mapToPermissionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get permission by ID
     */
    public PermissionResponse getPermissionById(Long id) {
        logger.info("Retrieving permission with ID: {}", id);
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with ID: " + id));
        return mapToPermissionResponse(permission);
    }

    /**
     * Get permission by name
     */
    public PermissionResponse getPermissionByName(String name) {
        logger.info("Retrieving permission with name: {}", name);
        Permission permission = permissionRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with name: " + name));
        return mapToPermissionResponse(permission);
    }

    /**
     * Update permission
     */
    @Transactional
    public PermissionResponse updatePermission(Long id, UpdatePermissionRequest request) {
        logger.info("Updating permission with ID: {}", id);

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with ID: " + id));

        // Check for conflicts if name is being updated
        if (request.getName() != null && !request.getName().equals(permission.getName())) {
            if (permissionRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("Permission with name '" + request.getName() + "' already exists");
            }
            permission.setName(request.getName());
        }

        // Update other fields if provided
        if (request.getResource() != null) {
            permission.setResource(request.getResource());
        }
        if (request.getAction() != null) {
            permission.setAction(request.getAction());
        }
        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }

        Permission updatedPermission = permissionRepository.save(permission);
        logger.info("Updated permission with ID: {}", updatedPermission.getId());

        return mapToPermissionResponse(updatedPermission);
    }

    /**
     * Delete permission
     */
    @Transactional
    public void deletePermission(Long id) {
        logger.info("Deleting permission with ID: {}", id);

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with ID: " + id));

        // Check if permission has associated role permissions
        if (!permission.getRolePermissions().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete permission with associated role permissions. Association count: " + permission.getRolePermissions().size());
        }

        permissionRepository.delete(permission);
        logger.info("Deleted permission with ID: {}", id);
    }

    /**
     * Check if permission exists by name
     */
    public boolean existsByName(String name) {
        return permissionRepository.existsByName(name);
    }

    /**
     * Get permissions by resource
     */
    public List<PermissionResponse> getPermissionsByResource(String resource) {
        logger.info("Retrieving permissions for resource: {}", resource);
        return permissionRepository.findByResource(resource).stream()
                .map(this::mapToPermissionResponse)
                .collect(Collectors.toList());
    }

    private PermissionResponse mapToPermissionResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .resource(permission.getResource())
                .action(permission.getAction())
                .description(permission.getDescription())
                .build();
    }
}
