package com.banking.transaction.approval;

import com.banking.account.module.entity.Account;
import com.banking.core.enums.TransactionType;
import com.banking.transaction.module.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Approval Context
 * 
 * Contains all information needed for transaction approval processing.
 * This object is passed through the approval chain.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalContext {

    private Transaction transaction;

    private BigDecimal amount;

    private TransactionType transactionType;

    private Long requesterId;

    private String requesterRole;

    private Account fromAccount;

    private Account toAccount;

    private String description;
}
