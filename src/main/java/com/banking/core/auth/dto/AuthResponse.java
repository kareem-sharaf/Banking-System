package com.banking.core.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Authentication Response DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;
    private String scope;
    private Map<String, Object> userInfo;
    private String message;
}
