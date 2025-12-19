package com.banking.account.service;

import com.banking.account.dto.DepositRequest;
import com.banking.account.dto.WithdrawalRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransferRequest;
import com.banking.account.module.entity.Account;
import com.banking.transaction.module.entity.Transaction;
import com.banking.core.enums.AccountEventType;
import com.banking.core.enums.AccountState;
import com.banking.core.enums.TransactionStatus;
import com.banking.core.enums.TransactionType;
import com.banking.core.enums.ApprovalStatus;
import com.banking.core.exception.AccountNotFoundException;
import com.banking.core.exception.InsufficientBalanceException;
import com.banking.core.exception.InvalidAccountStateException;
import com.banking.account.service.notification.AccountEvent;
import com.banking.account.service.notification.AccountSubjectManager;
import com.banking.account.repository.AccountRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.service.TransactionApprovalService;
import com.banking.transaction.approval.ApprovalResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Account Service
 * 
 * Service layer for account operations with Observer Pattern integration.
 * Handles deposits, withdrawals, and transfers while notifying observers
 * about account events.
 * 
 * Uses @Transactional to ensure data consistency.
 * 
 * @author Banking System
 */
@Service
@RequiredArgsConstructor
public class AccountService {

        private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

        private final AccountRepository accountRepository;
        private final TransactionRepository transactionRepository;
        private final AccountSubjectManager accountSubjectManager;
        private final TransactionApprovalService transactionApprovalService;

        /**
         * Deposit money into an account
         * 
         * This method:
         * 1. Validates the account exists and is in valid state
         * 2. Performs the deposit
         * 3. Creates a transaction record
         * 4. Notifies all observers about the deposit event
         * 
         * @param accountNumber The account number to deposit to
         * @param request       The deposit request containing amount and optional
         *                      details
         * @return TransactionResponse with transaction details
         */
        @Transactional
        public TransactionResponse deposit(String accountNumber, DepositRequest request) {
                logger.info("Processing deposit request for account: {}", accountNumber);

                // 1. Find account
                Account account = accountRepository.findByAccountNumber(accountNumber)
                                .orElseThrow(() -> new AccountNotFoundException(
                                                "Account not found with number: " + accountNumber));

                // 2. Validate account state
                validateAccountState(account, "deposit");

                // 3. Store previous balance for event
                BigDecimal previousBalance = account.getBalance();
                BigDecimal depositAmount = request.getAmount();

                // 4. Create transaction record (initially as PENDING)
                Transaction transaction = createTransaction(
                                account,
                                null, // No destination account for deposit
                                TransactionType.DEPOSIT,
                                depositAmount,
                                request.getDescription() != null ? request.getDescription() : "Deposit",
                                TransactionStatus.PENDING);

                // 5. Process approval through chain
                ApprovalResult approvalResult = transactionApprovalService.processApproval(transaction);

                // 6. Handle approval result
                BigDecimal newBalance;
                if (approvalResult.isApproved() && approvalResult.getStatus() == ApprovalStatus.AUTO_APPROVED) {
                        // Auto-approved: Execute the deposit
                        newBalance = previousBalance.add(depositAmount);
                        account.setBalance(newBalance);
                        account.setUpdatedAt(LocalDateTime.now());
                        transaction.setStatus(TransactionStatus.COMPLETED);
                } else if (approvalResult.isPending()) {
                        // Pending approval: Don't execute, keep transaction as PENDING
                        newBalance = previousBalance; // Balance unchanged
                        logger.info("Deposit transaction {} is pending approval", transaction.getTransactionNumber());
                } else if (approvalResult.isRejected()) {
                        // Rejected: Don't execute
                        throw new IllegalStateException("Transaction rejected: " + approvalResult.getMessage());
                } else {
                        // Should not happen, but handle gracefully
                        newBalance = previousBalance;
                        transaction.setStatus(TransactionStatus.PENDING);
                }

                transaction = transactionRepository.save(transaction);
                account = accountRepository.save(account);

                // 7. Only notify observers if transaction was auto-approved and completed
                if (approvalResult.isApproved() && approvalResult.getStatus() == ApprovalStatus.AUTO_APPROVED) {
                        // Attach observers if not already attached
                        accountSubjectManager.attachAllObservers(account);

                        // Create and notify observers about the event
                        AccountEvent event = AccountEvent.builder()
                                        .eventType(AccountEventType.DEPOSIT_COMPLETED)
                                        .account(account)
                                        .transaction(transaction)
                                        .timestamp(LocalDateTime.now())
                                        .amount(depositAmount)
                                        .previousBalance(previousBalance)
                                        .newBalance(newBalance)
                                        .message("Deposit of " + depositAmount + " " + account.getCurrency()
                                                        + " completed successfully")
                                        .build();

                        accountSubjectManager.notifyObservers(event);

                        logger.info("Deposit completed successfully for account: {}. New balance: {}",
                                        accountNumber, newBalance);
                }

                // 8. Return response
                String message = approvalResult.isApproved()
                                ? "Deposit completed successfully"
                                : "Deposit request submitted and pending approval";

                return TransactionResponse.builder()
                                .success(approvalResult.isApproved())
                                .message(message)
                                .newBalance(newBalance)
                                .transactionNumber(transaction.getTransactionNumber())
                                .accountNumber(account.getAccountNumber())
                                .timestamp(transaction.getTransactionDate())
                                .currency(account.getCurrency())
                                .amount(depositAmount)
                                .build();
        }

        /**
         * Withdraw money from an account
         * 
         * @param accountNumber The account number to withdraw from
         * @param request       The withdrawal request
         * @return TransactionResponse
         */
        @Transactional
        public TransactionResponse withdraw(String accountNumber, WithdrawalRequest request) {
                logger.info("Processing withdrawal request for account: {}", accountNumber);

                // 1. Find account
                Account account = accountRepository.findByAccountNumber(accountNumber)
                                .orElseThrow(() -> new AccountNotFoundException(
                                                "Account not found with number: " + accountNumber));

                // 2. Validate account state
                validateAccountState(account, "withdraw");

                // 3. Check sufficient balance
                BigDecimal withdrawalAmount = request.getAmount();
                BigDecimal currentBalance = account.getBalance();

                if (currentBalance.compareTo(withdrawalAmount) < 0) {
                        throw new InsufficientBalanceException(
                                        String.format("Insufficient balance. Current balance: %s %s, Requested: %s %s",
                                                        currentBalance, account.getCurrency(),
                                                        withdrawalAmount, account.getCurrency()));
                }

                // 4. Store previous balance
                BigDecimal previousBalance = currentBalance;

                // 5. Create transaction record (initially as PENDING)
                Transaction transaction = createTransaction(
                                account,
                                null, // No destination account for withdrawal
                                TransactionType.WITHDRAWAL,
                                withdrawalAmount,
                                request.getDescription() != null ? request.getDescription() : "Withdrawal",
                                TransactionStatus.PENDING);

                // 6. Process approval through chain
                ApprovalResult approvalResult = transactionApprovalService.processApproval(transaction);

                // 7. Handle approval result
                BigDecimal newBalance;
                if (approvalResult.isApproved() && approvalResult.getStatus() == ApprovalStatus.AUTO_APPROVED) {
                        // Auto-approved: Execute the withdrawal
                        newBalance = previousBalance.subtract(withdrawalAmount);
                        account.setBalance(newBalance);
                        account.setUpdatedAt(LocalDateTime.now());
                        transaction.setStatus(TransactionStatus.COMPLETED);
                } else if (approvalResult.isPending()) {
                        // Pending approval: Don't execute, keep transaction as PENDING
                        newBalance = previousBalance; // Balance unchanged
                        logger.info("Withdrawal transaction {} is pending approval",
                                        transaction.getTransactionNumber());
                } else if (approvalResult.isRejected()) {
                        // Rejected: Don't execute
                        throw new IllegalStateException("Transaction rejected: " + approvalResult.getMessage());
                } else {
                        // Should not happen, but handle gracefully
                        newBalance = previousBalance;
                        transaction.setStatus(TransactionStatus.PENDING);
                }

                transaction = transactionRepository.save(transaction);
                account = accountRepository.save(account);

                // 8. Only notify observers if transaction was auto-approved and completed
                if (approvalResult.isApproved() && approvalResult.getStatus() == ApprovalStatus.AUTO_APPROVED) {
                        // Attach observers if not already attached
                        accountSubjectManager.attachAllObservers(account);

                        // Create and notify observers
                        AccountEvent event = AccountEvent.builder()
                                        .eventType(AccountEventType.WITHDRAWAL_COMPLETED)
                                        .account(account)
                                        .transaction(transaction)
                                        .timestamp(LocalDateTime.now())
                                        .amount(withdrawalAmount)
                                        .previousBalance(previousBalance)
                                        .newBalance(newBalance)
                                        .message("Withdrawal of " + withdrawalAmount + " " + account.getCurrency()
                                                        + " completed successfully")
                                        .build();

                        accountSubjectManager.notifyObservers(event);

                        logger.info("Withdrawal completed successfully for account: {}. New balance: {}",
                                        accountNumber, newBalance);

                        // Check for low balance and notify if needed
                        checkAndNotifyLowBalance(account, newBalance);
                }

                // 9. Return response
                String message = approvalResult.isApproved()
                                ? "Withdrawal completed successfully"
                                : "Withdrawal request submitted and pending approval";

                return TransactionResponse.builder()
                                .success(approvalResult.isApproved())
                                .message(message)
                                .newBalance(newBalance)
                                .transactionNumber(transaction.getTransactionNumber())
                                .accountNumber(account.getAccountNumber())
                                .timestamp(transaction.getTransactionDate())
                                .currency(account.getCurrency())
                                .amount(withdrawalAmount)
                                .build();
        }

        /**
         * Transfer money between accounts
         * 
         * @param fromAccountNumber The source account number
         * @param request           The transfer request
         * @return TransactionResponse
         */
        @Transactional
        public TransactionResponse transfer(String fromAccountNumber, TransferRequest request) {
                logger.info("Processing transfer request from account: {} to account: {}",
                                fromAccountNumber, request.getToAccountNumber());

                // 1. Find source account
                Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                                .orElseThrow(() -> new AccountNotFoundException(
                                                "Source account not found with number: " + fromAccountNumber));

                // 2. Find destination account
                Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                                .orElseThrow(() -> new AccountNotFoundException(
                                                "Destination account not found with number: "
                                                                + request.getToAccountNumber()));

                // 3. Validate accounts are different
                if (fromAccount.getId().equals(toAccount.getId())) {
                        throw new IllegalArgumentException("Cannot transfer to the same account");
                }

                // 4. Validate source account state
                validateAccountState(fromAccount, "transfer from");

                // 5. Validate destination account state
                validateAccountState(toAccount, "transfer to");

                // 6. Check sufficient balance in source account
                BigDecimal transferAmount = request.getAmount();
                BigDecimal sourceBalance = fromAccount.getBalance();

                if (sourceBalance.compareTo(transferAmount) < 0) {
                        throw new InsufficientBalanceException(
                                        String.format("Insufficient balance in source account. Current balance: %s %s, Requested: %s %s",
                                                        sourceBalance, fromAccount.getCurrency(),
                                                        transferAmount, fromAccount.getCurrency()));
                }

                // 7. Store previous balances
                BigDecimal fromPreviousBalance = fromAccount.getBalance();
                BigDecimal toPreviousBalance = toAccount.getBalance();

                // 8. Create transaction record (initially as PENDING)
                Transaction transaction = createTransaction(
                                fromAccount,
                                toAccount,
                                TransactionType.TRANSFER,
                                transferAmount,
                                request.getDescription() != null ? request.getDescription() : "Transfer",
                                TransactionStatus.PENDING);

                // 9. Process approval through chain
                ApprovalResult approvalResult = transactionApprovalService.processApproval(transaction);

                // 10. Handle approval result
                BigDecimal fromNewBalance;
                BigDecimal toNewBalance;
                if (approvalResult.isApproved() && approvalResult.getStatus() == ApprovalStatus.AUTO_APPROVED) {
                        // Auto-approved: Execute the transfer
                        fromNewBalance = fromPreviousBalance.subtract(transferAmount);
                        toNewBalance = toPreviousBalance.add(transferAmount);
                        fromAccount.setBalance(fromNewBalance);
                        fromAccount.setUpdatedAt(LocalDateTime.now());
                        toAccount.setBalance(toNewBalance);
                        toAccount.setUpdatedAt(LocalDateTime.now());
                        transaction.setStatus(TransactionStatus.COMPLETED);
                } else if (approvalResult.isPending()) {
                        // Pending approval: Don't execute, keep transaction as PENDING
                        fromNewBalance = fromPreviousBalance; // Balance unchanged
                        toNewBalance = toPreviousBalance;
                        logger.info("Transfer transaction {} is pending approval", transaction.getTransactionNumber());
                } else if (approvalResult.isRejected()) {
                        // Rejected: Don't execute
                        throw new IllegalStateException("Transaction rejected: " + approvalResult.getMessage());
                } else {
                        // Should not happen, but handle gracefully
                        fromNewBalance = fromPreviousBalance;
                        toNewBalance = toPreviousBalance;
                        transaction.setStatus(TransactionStatus.PENDING);
                }

                transaction = transactionRepository.save(transaction);
                fromAccount = accountRepository.save(fromAccount);
                toAccount = accountRepository.save(toAccount);

                // 11. Only notify observers if transaction was auto-approved and completed
                if (approvalResult.isApproved() && approvalResult.getStatus() == ApprovalStatus.AUTO_APPROVED) {
                        // Attach observers
                        accountSubjectManager.attachAllObservers(fromAccount);
                        accountSubjectManager.attachAllObservers(toAccount);

                        // Notify observers for source account
                        AccountEvent fromEvent = AccountEvent.builder()
                                        .eventType(AccountEventType.TRANSFER_COMPLETED)
                                        .account(fromAccount)
                                        .transaction(transaction)
                                        .timestamp(LocalDateTime.now())
                                        .amount(transferAmount)
                                        .previousBalance(fromPreviousBalance)
                                        .newBalance(fromNewBalance)
                                        .message("Transfer of " + transferAmount + " " + fromAccount.getCurrency() +
                                                        " to account " + toAccount.getAccountNumber() + " completed")
                                        .build();

                        accountSubjectManager.notifyObservers(fromEvent);

                        // Notify observers for destination account
                        AccountEvent toEvent = AccountEvent.builder()
                                        .eventType(AccountEventType.DEPOSIT_COMPLETED)
                                        .account(toAccount)
                                        .transaction(transaction)
                                        .timestamp(LocalDateTime.now())
                                        .amount(transferAmount)
                                        .previousBalance(toPreviousBalance)
                                        .newBalance(toNewBalance)
                                        .message("Transfer received of " + transferAmount + " "
                                                        + toAccount.getCurrency() +
                                                        " from account " + fromAccount.getAccountNumber())
                                        .build();

                        accountSubjectManager.notifyObservers(toEvent);

                        logger.info("Transfer completed successfully from account: {} to account: {}",
                                        fromAccountNumber, request.getToAccountNumber());

                        // Check for low balance
                        checkAndNotifyLowBalance(fromAccount, fromNewBalance);
                }

                // 12. Return response
                String message = approvalResult.isApproved()
                                ? "Transfer completed successfully"
                                : "Transfer request submitted and pending approval";

                return TransactionResponse.builder()
                                .success(approvalResult.isApproved())
                                .message(message)
                                .newBalance(fromNewBalance)
                                .transactionNumber(transaction.getTransactionNumber())
                                .accountNumber(fromAccount.getAccountNumber())
                                .timestamp(transaction.getTransactionDate())
                                .currency(fromAccount.getCurrency())
                                .amount(transferAmount)
                                .build();
        }

        /**
         * Validate account state for operations
         */
        private void validateAccountState(Account account, String operation) {
                if (account.getState() != AccountState.ACTIVE) {
                        throw new InvalidAccountStateException(
                                        String.format("Cannot %s: Account %s is in %s state",
                                                        operation, account.getAccountNumber(), account.getState()));
                }
        }

        /**
         * Create a transaction record
         */
        private Transaction createTransaction(Account fromAccount, Account toAccount,
                        TransactionType type, BigDecimal amount,
                        String description, TransactionStatus status) {
                Transaction transaction = new Transaction();
                transaction.setTransactionNumber(generateTransactionNumber());
                transaction.setFromAccount(fromAccount);
                transaction.setToAccount(toAccount);
                transaction.setTransactionType(type);
                transaction.setAmount(amount);
                transaction.setCurrency(fromAccount.getCurrency());
                transaction.setTransactionDate(LocalDateTime.now());
                transaction.setStatus(status);
                transaction.setDescription(description);
                return transaction;
        }

        /**
         * Generate unique transaction number
         */
        private String generateTransactionNumber() {
                return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() +
                                "-" + System.currentTimeMillis();
        }

        /**
         * Check for low balance and notify if threshold is reached
         */
        private void checkAndNotifyLowBalance(Account account, BigDecimal balance) {
                // Define low balance threshold (e.g., 100)
                BigDecimal lowBalanceThreshold = new BigDecimal("100");

                if (balance.compareTo(lowBalanceThreshold) < 0) {
                        AccountEvent event = AccountEvent.builder()
                                        .eventType(AccountEventType.LOW_BALANCE)
                                        .account(account)
                                        .timestamp(LocalDateTime.now())
                                        .newBalance(balance)
                                        .message("Account balance is below threshold: " + lowBalanceThreshold + " "
                                                        + account.getCurrency())
                                        .build();

                        accountSubjectManager.notifyObservers(event);
                        logger.info("Low balance alert triggered for account: {}", account.getAccountNumber());
                }
        }
}
