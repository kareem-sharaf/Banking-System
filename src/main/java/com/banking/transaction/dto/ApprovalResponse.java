package com.banking.transaction.dto;

import com.banking.core.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Approval Response DTO
 * 
 * Response object for approval operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalResponse {

    /**
     * Success flag
     */
    private boolean success;

    /**
     * Response message
     */
    private String message;

    /**
     * Approval status
     */
    private ApprovalStatus approvalStatus;

    /**
     * Transaction ID
     */
    private Long transactionId;

    /**
     * Transaction number
     */
    private String transactionNumber;

    /**
     * ID of the user who approved/rejected
     */
    private Long approvedBy;

    /**
     * Name of the approver
     */
    private String approverName;

    /**
     * Timestamp of approval/rejection
     */
    private LocalDateTime approvedAt;

    /**
     * Comment or reason
     */
    private String comment;

    /**
     * Handler used for processing
     */
    private String handlerUsed;
}
