package com.banking.core.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update Permission Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePermissionRequest {

    @Size(max = 100, message = "Permission name must not exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Resource must not exceed 100 characters")
    private String resource;

    @Size(max = 50, message = "Action must not exceed 50 characters")
    private String action;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
