package com.banking.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Account Type Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTypeResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer accountCount;
}
