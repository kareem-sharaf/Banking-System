package com.banking.customer.dto;

import com.banking.core.enums.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Customer Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private Long id;
    private Long userId;
    private String customerNumber;
    private LocalDateTime joinDate;
    private CustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer accountCount;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
}
