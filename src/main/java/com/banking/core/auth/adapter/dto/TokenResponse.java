package com.banking.core.auth.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Common token response DTO for authentication providers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private Instant expiresAt;
    private String scope;
    private boolean success;
    private String error;
    private String errorDescription;
    private String message;

    /**
     * Check if the token response indicates success
     */
    public boolean isSuccess() {
        return success && accessToken != null && !accessToken.isEmpty();
    }

    /**
     * Check if the token response indicates an error
     */
    public boolean hasError() {
        return error != null || !success;
    }
}
