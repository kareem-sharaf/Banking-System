package com.banking.customer.dto;

import com.banking.core.enums.CustomerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create Customer Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private CustomerStatus status = CustomerStatus.ACTIVE;
}
