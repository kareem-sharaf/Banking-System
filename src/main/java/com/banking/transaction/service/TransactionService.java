package com.banking.transaction.service;

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
 * Transaction Service
 * 
 * Handles all transaction-related business logic: Deposits, Withdrawals, Transfers.
 * Moves this logic out of AccountService to separate concerns.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountSubjectManager accountSubjectManager;
    private final TransactionApprovalService transactionApprovalService;

    /**
     * Deposit money
     */
    @Transactional
    public TransactionResponse deposit(String accountNumber, DepositRequest request) {
        logger.info("Processing deposit request for account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        validateAccountState(account, "deposit");

        BigDecimal previousBalance = account.getBalance();
        BigDecimal depositAmount = request.getAmount();

        Transaction transaction = createTransaction(
                account, null, TransactionType.DEPOSIT, depositAmount,
                request.getDescription() != null ? request.getDescription() : "Deposit",
                TransactionStatus.PENDING);

        ApprovalResult approvalResult = transactionApprovalService.processApproval(transaction);

        BigDecimal newBalance = previousBalance;
        if (approvalResult.isApproved() && approvalResult.getStatus() == ApprovalStatus.AUTO_APPROVED) {
            newBalance = previousBalance.add(depositAmount);
            account.setBalance(newBalance);
            transaction.setStatus(TransactionStatus.COMPLETED);
        } else if (approvalResult.isRejected()) {
            throw new IllegalStateException("Transaction rejected: " + approvalResult.getMessage());
        }

        transaction = transactionRepository.save(transaction);
        account = accountRepository.save(account);

        if (approvalResult.isApproved() && approvalResult.getStatus() == ApprovalStatus.AUTO_APPROVED) {
            notifyObservers(account, transaction, AccountEventType.DEPOSIT_COMPLETED, depositAmount, previousBalance, newBalance, "Deposit completed");
        }

        return buildResponse(transaction, account, newBalance, approvalResult, "Deposit");
    }

    /**
     * Withdraw money
     */
    @Transactional
    public TransactionResponse withdraw(String accountNumber, WithdrawalRequest request) {
        logger.info("Processing withdrawal request for account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        validateAccountState(account, "withdraw");

        BigDecimal currentBalance = account.getBalance();
        BigDecimal withdrawalAmount = request.getAmount();

        if (currentBalance.compareTo(withdrawalAmount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        BigDecimal previousBalance = currentBalance;

        Transaction transaction = createTransaction(
                account, null, TransactionType.WITHDRAWAL, withdrawalAmount,
                request.getDescription() != null ? request.getDescription() : "Withdrawal",
                TransactionStatus.PENDING);

        ApprovalResult approvalResult = transactionApprovalService.processApproval(transaction);

        BigDecimal newBalance = previousBalance;
        if (approvalResult.isApproved() && approvalResult.getStatus() == ApprovalStatus.AUTO_APPROVED) {
            newBalance = previousBalance.subtract(withdrawalAmount);
            account.setBalance(newBalance);
            transaction.setStatus(TransactionStatus.COMPLETED);
        } else if (approvalResult.isRejected()) {
            throw new IllegalStateException("Transaction rejected: " + approvalResult.getMessage());
        }

        transaction = transactionRepository.save(transaction);
        account = accountRepository.save(account);

        if (approvalResult.isApproved() && approvalResult.getStatus() == ApprovalStatus.AUTO_APPROVED) {
            notifyObservers(account, transaction, AccountEventType.WITHDRAWAL_COMPLETED, withdrawalAmount, previousBalance, newBalance, "Withdrawal completed");
            checkAndNotifyLowBalance(account, newBalance);
        }

        return buildResponse(transaction, account, newBalance, approvalResult, "Withdrawal");
    }

    /**
     * Transfer money
     */
    @Transactional
    public TransactionResponse transfer(String fromAccountNumber, TransferRequest request) {
        logger.info("Processing transfer request from: {} to: {}", fromAccountNumber, request.getToAccountNumber());

        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + fromAccountNumber));
        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + request.getToAccountNumber()));

        validateAccountState(fromAccount, "transfer from");
        validateAccountState(toAccount, "transfer to");

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in source account");
        }

        BigDecimal transferAmount = request.getAmount();
        BigDecimal fromPrevious = fromAccount.getBalance();
        BigDecimal toPrevious = toAccount.getBalance();

        Transaction transaction = createTransaction(
                fromAccount, toAccount, TransactionType.TRANSFER, transferAmount,
                request.getDescription() != null ? request.getDescription() : "Transfer",
                TransactionStatus.PENDING);

        ApprovalResult approvalResult = transactionApprovalService.processApproval(transaction);

        BigDecimal fromNew = fromPrevious;
        if (approvalResult.isApproved() && approvalResult.getStatus() == ApprovalStatus.AUTO_APPROVED) {
            fromNew = fromPrevious.subtract(transferAmount);
            BigDecimal toNew = toPrevious.add(transferAmount);
            fromAccount.setBalance(fromNew);
            toAccount.setBalance(toNew);
            transaction.setStatus(TransactionStatus.COMPLETED);
        } else if (approvalResult.isRejected()) {
            throw new IllegalStateException("Transaction rejected: " + approvalResult.getMessage());
        }

        transaction = transactionRepository.save(transaction);
        fromAccount = accountRepository.save(fromAccount);
        toAccount = accountRepository.save(toAccount);

        if (approvalResult.isApproved() && approvalResult.getStatus() == ApprovalStatus.AUTO_APPROVED) {
            notifyObservers(fromAccount, transaction, AccountEventType.TRANSFER_COMPLETED, transferAmount, fromPrevious, fromNew, "Transfer sent");
            
            // Notify destination
            AccountEvent toEvent = AccountEvent.builder()
                    .eventType(AccountEventType.DEPOSIT_COMPLETED)
                    .account(toAccount)
                    .transaction(transaction)
                    .timestamp(LocalDateTime.now())
                    .amount(transferAmount)
                    .previousBalance(toPrevious)
                    .newBalance(toAccount.getBalance())
                    .message("Transfer received")
                    .build();
            accountSubjectManager.attachAllObservers(toAccount);
            accountSubjectManager.notifyObservers(toEvent);

            checkAndNotifyLowBalance(fromAccount, fromNew);
        }

        return buildResponse(transaction, fromAccount, fromNew, approvalResult, "Transfer");
    }

    // Helper Methods

    private void validateAccountState(Account account, String operation) {
        if (account.getState() != AccountState.ACTIVE) {
            throw new InvalidAccountStateException("Cannot " + operation + ": Account is " + account.getState());
        }
    }

    private Transaction createTransaction(Account from, Account to, TransactionType type, BigDecimal amount, String desc, TransactionStatus status) {
        Transaction tx = new Transaction();
        tx.setTransactionNumber("TXN-" + UUID.randomUUID().toString().substring(0,8).toUpperCase() + "-" + System.currentTimeMillis());
        tx.setFromAccount(from);
        tx.setToAccount(to);
        tx.setTransactionType(type);
        tx.setAmount(amount);
        tx.setCurrency(from.getCurrency());
        tx.setTransactionDate(LocalDateTime.now());
        tx.setStatus(status);
        tx.setDescription(desc);
        return tx;
    }

    private void notifyObservers(Account account, Transaction tx, AccountEventType type, BigDecimal amount, BigDecimal oldBal, BigDecimal newBal, String msg) {
        accountSubjectManager.attachAllObservers(account);
        AccountEvent event = AccountEvent.builder()
                .eventType(type)
                .account(account)
                .transaction(tx)
                .timestamp(LocalDateTime.now())
                .amount(amount)
                .previousBalance(oldBal)
                .newBalance(newBal)
                .message(msg)
                .build();
        accountSubjectManager.notifyObservers(event);
    }

    private void checkAndNotifyLowBalance(Account account, BigDecimal balance) {
        if (balance.compareTo(new BigDecimal("100")) < 0) {
            notifyObservers(account, null, AccountEventType.LOW_BALANCE, null, null, balance, "Low balance alert");
        }
    }

    private TransactionResponse buildResponse(Transaction tx, Account acc, BigDecimal balance, ApprovalResult result, String type) {
        String msg = result.isApproved() ? type + " completed successfully" : type + " pending approval";
        return TransactionResponse.builder()
                .success(result.isApproved())
                .message(msg)
                .newBalance(balance)
                .transactionNumber(tx.getTransactionNumber())
                .accountNumber(acc.getAccountNumber())
                .timestamp(tx.getTransactionDate())
                .currency(acc.getCurrency())
                .amount(tx.getAmount())
                .build();
    }
}
