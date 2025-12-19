package com.banking.transaction.approval;

import com.banking.core.enums.ApprovalStatus;
import com.banking.transaction.module.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Approval Result
 * 
 * Represents the result of an approval process through the chain.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalResult {

    private ApprovalStatus status;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private Long rejectedBy;

    private LocalDateTime rejectedAt;

    private String comment;

    private Transaction transaction;

    private String handlerUsed;

    private String message;

    public boolean isApproved() {
        return status == ApprovalStatus.APPROVED || status == ApprovalStatus.AUTO_APPROVED;
    }

    public boolean isPending() {
        return status == ApprovalStatus.PENDING;
    }

    public boolean isRejected() {
        return status == ApprovalStatus.REJECTED;
    }
}
