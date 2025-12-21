package com.banking.core.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common user registration result DTO for authentication providers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationResult {
    private boolean success;
    private String userId;
    private String message;
    private String error;
    private String errorDescription;
}

