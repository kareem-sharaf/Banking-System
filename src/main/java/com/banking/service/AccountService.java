package com.banking.service;

import com.banking.dto.DepositRequest;
import com.banking.dto.TransactionResponse;
import com.banking.dto.TransferRequest;
import com.banking.dto.WithdrawalRequest;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.enums.AccountEventType;
import com.banking.enums.AccountState;
import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidAccountStateException;
import com.banking.observer.event.AccountEvent;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
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

                // 4. Perform deposit
                BigDecimal depositAmount = request.getAmount();
                BigDecimal newBalance = previousBalance.add(depositAmount);
                account.setBalance(newBalance);
                account.setUpdatedAt(LocalDateTime.now());

                // 5. Create transaction record
                Transaction transaction = createTransaction(
                                account,
                                null, // No destination account for deposit
                                TransactionType.DEPOSIT,
                                depositAmount,
                                request.getDescription() != null ? request.getDescription() : "Deposit",
                                TransactionStatus.COMPLETED);

                transaction = transactionRepository.save(transaction);
                account = accountRepository.save(account);

                // 6. Attach observers if not already attached
                accountSubjectManager.attachAllObservers(account);

                // 7. Create and notify observers about the event
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

                // 8. Return response
                return TransactionResponse.builder()
                                .success(true)
                                .message("Deposit completed successfully")
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

                // 5. Perform withdrawal
                BigDecimal newBalance = previousBalance.subtract(withdrawalAmount);
                account.setBalance(newBalance);
                account.setUpdatedAt(LocalDateTime.now());

                // 6. Create transaction record
                Transaction transaction = createTransaction(
                                account,
                                null, // No destination account for withdrawal
                                TransactionType.WITHDRAWAL,
                                withdrawalAmount,
                                request.getDescription() != null ? request.getDescription() : "Withdrawal",
                                TransactionStatus.COMPLETED);

                transaction = transactionRepository.save(transaction);
                account = accountRepository.save(account);

                // 7. Attach observers if not already attached
                accountSubjectManager.attachAllObservers(account);

                // 8. Create and notify observers
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

                // 9. Check for low balance and notify if needed
                checkAndNotifyLowBalance(account, newBalance);

                // 10. Return response
                return TransactionResponse.builder()
                                .success(true)
                                .message("Withdrawal completed successfully")
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

                // 8. Perform transfer
                BigDecimal fromNewBalance = fromPreviousBalance.subtract(transferAmount);
                BigDecimal toNewBalance = toPreviousBalance.add(transferAmount);

                fromAccount.setBalance(fromNewBalance);
                fromAccount.setUpdatedAt(LocalDateTime.now());
                toAccount.setBalance(toNewBalance);
                toAccount.setUpdatedAt(LocalDateTime.now());

                // 9. Create transaction record
                Transaction transaction = createTransaction(
                                fromAccount,
                                toAccount,
                                TransactionType.TRANSFER,
                                transferAmount,
                                request.getDescription() != null ? request.getDescription() : "Transfer",
                                TransactionStatus.COMPLETED);

                transaction = transactionRepository.save(transaction);
                fromAccount = accountRepository.save(fromAccount);
                toAccount = accountRepository.save(toAccount);

                // 10. Attach observers
                accountSubjectManager.attachAllObservers(fromAccount);
                accountSubjectManager.attachAllObservers(toAccount);

                // 11. Notify observers for source account
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

                // 12. Notify observers for destination account
                AccountEvent toEvent = AccountEvent.builder()
                                .eventType(AccountEventType.DEPOSIT_COMPLETED)
                                .account(toAccount)
                                .transaction(transaction)
                                .timestamp(LocalDateTime.now())
                                .amount(transferAmount)
                                .previousBalance(toPreviousBalance)
                                .newBalance(toNewBalance)
                                .message("Transfer received of " + transferAmount + " " + toAccount.getCurrency() +
                                                " from account " + fromAccount.getAccountNumber())
                                .build();

                accountSubjectManager.notifyObservers(toEvent);

                logger.info("Transfer completed successfully from account: {} to account: {}",
                                fromAccountNumber, request.getToAccountNumber());

                // 13. Check for low balance
                checkAndNotifyLowBalance(fromAccount, fromNewBalance);

                // 14. Return response
                return TransactionResponse.builder()
                                .success(true)
                                .message("Transfer completed successfully")
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
