package com.banking.account.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAccountRequest {
    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Account Type is required")
    private String accountTypeCode; // SAVINGS, CHECKING, etc.

    private BigDecimal initialDeposit;
    
    private String currency; // USD, EUR
}
