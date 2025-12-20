package com.banking.transaction.facade;

import com.banking.account.dto.DepositRequest;
import com.banking.account.dto.WithdrawalRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransferRequest;

/**
 * Transaction Facade
 * 
 * Unified interface for handling all financial transactions (deposits, withdrawals, transfers).
 * Orchestrates operations across Account, Transaction, and Notification subsystems.
 */
public interface TransactionFacade {
    
    /**
     * Process a deposit transaction.
     */
    TransactionResponse deposit(String accountNumber, DepositRequest request);

    /**
     * Process a withdrawal transaction.
     */
    TransactionResponse withdraw(String accountNumber, WithdrawalRequest request);

    /**
     * Process a transfer transaction.
     */
    TransactionResponse transfer(String fromAccountNumber, TransferRequest request);
}
