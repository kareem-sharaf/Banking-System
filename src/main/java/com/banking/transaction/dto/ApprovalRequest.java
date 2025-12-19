package com.banking.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Approval Request DTO
 * 
 * Used for manual approval/rejection of transactions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    /**
     * Transaction ID to approve/reject
     */
    @NotNull(message = "Transaction ID is required")
    private Long transactionId;

    /**
     * Comment or reason for approval/rejection
     */
    @NotBlank(message = "Comment is required")
    private String comment;
}
