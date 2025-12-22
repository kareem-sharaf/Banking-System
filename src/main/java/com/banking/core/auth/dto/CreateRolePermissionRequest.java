package com.banking.core.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create Role Permission Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRolePermissionRequest {

    @NotNull(message = "Role ID is required")
    private Long roleId;

    @NotNull(message = "Permission ID is required")
    private Long permissionId;
}
