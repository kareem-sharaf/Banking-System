package com.banking.core.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Role Permission Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionResponse {
    private Long id;
    private Long roleId;
    private String roleName;
    private Long permissionId;
    private String permissionName;
    private String permissionResource;
    private String permissionAction;
}
