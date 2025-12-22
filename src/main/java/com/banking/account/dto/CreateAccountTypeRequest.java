package com.banking.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create Account Type Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountTypeRequest {

    @NotBlank(message = "Account type name is required")
    @Size(max = 50, message = "Account type name must not exceed 50 characters")
    private String name;

    @NotBlank(message = "Account type code is required")
    @Size(max = 20, message = "Account type code must not exceed 20 characters")
    private String code;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
