package com.banking.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Withdrawal Request DTO
 * 
 * Data Transfer Object for withdrawal requests.
 * Includes validation annotations to ensure data integrity.
 * 
 * @author Banking System
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequest {

    /**
     * The amount to withdraw
     * Must be positive and greater than zero
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    /**
     * Optional reference or description for the withdrawal
     */
    private String reference;

    /**
     * Optional description for the transaction
     */
    private String description;
}
