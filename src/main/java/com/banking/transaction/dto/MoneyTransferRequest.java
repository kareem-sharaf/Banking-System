package com.banking.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Money Transfer Request
 * 
 * Extended Transfer Request that includes the source account number.
 * Used for the standalone transfer endpoint.
 */
@Getter
@Setter
public class MoneyTransferRequest extends TransferRequest {

    /**
     * The source account number to transfer from
     */
    @NotBlank(message = "Source account number is required")
    private String fromAccountNumber;
}
