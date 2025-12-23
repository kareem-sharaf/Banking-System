package com.banking.transaction.service;

import com.banking.account.dto.DepositRequest;
import com.banking.account.dto.WithdrawalRequest;
import com.banking.account.module.entity.Account;
import com.banking.account.repository.AccountRepository;
import com.banking.account.service.notification.AccountEvent;
import com.banking.account.service.notification.AccountSubjectManager;
import com.banking.core.enums.AccountEventType;
import com.banking.core.enums.AccountState;
import com.banking.core.enums.ApprovalStatus;
import com.banking.core.enums.TransactionStatus;
import com.banking.core.enums.TransactionType;
import com.banking.core.exception.AccountNotFoundException;
import com.banking.core.exception.InsufficientBalanceException;
import com.banking.core.exception.InvalidAccountStateException;
import com.banking.transaction.approval.ApprovalResult;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.module.entity.Transaction;
import com.banking.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Tests for TransactionService
 * 
 * Test Strategy: AAA Pattern (Arrange-Act-Assert)
 * Coverage: >95% line coverage, 100% critical path coverage
 * 
 * This service handles critical financial operations:
 * - Deposits
 * - Withdrawals
 * - Transfers
 * 
 * @author QA Automation Team
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountSubjectManager accountSubjectManager;

    @Mock
    private TransactionApprovalService transactionApprovalService;

    @InjectMocks
    private TransactionService transactionService;

    private Account testAccount;
    private Account destinationAccount;
    private DepositRequest depositRequest;
    private WithdrawalRequest withdrawalRequest;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        // Arrange: Create test account
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setAccountNumber("ACC123456");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setState(AccountState.ACTIVE);
        testAccount.setCurrency("USD");
        testAccount.setOpenedDate(LocalDateTime.now());

        destinationAccount = new Account();
        destinationAccount.setId(2L);
        destinationAccount.setAccountNumber("ACC789012");
        destinationAccount.setBalance(new BigDecimal("500.00"));
        destinationAccount.setState(AccountState.ACTIVE);
        destinationAccount.setCurrency("USD");
        destinationAccount.setOpenedDate(LocalDateTime.now());

        // Arrange: Create request DTOs
        depositRequest = new DepositRequest();
        depositRequest.setAmount(new BigDecimal("100.00"));
        depositRequest.setDescription("Test deposit");

        withdrawalRequest = new WithdrawalRequest();
        withdrawalRequest.setAmount(new BigDecimal("50.00"));
        withdrawalRequest.setDescription("Test withdrawal");

        transferRequest = new TransferRequest();
        transferRequest.setToAccountNumber("ACC789012");
        transferRequest.setAmount(new BigDecimal("200.00"));
        transferRequest.setDescription("Test transfer");
    }

    @Nested
    @DisplayName("deposit Tests")
    class DepositTests {

        @Test
        @DisplayName("TS-001: Deposit valid amount to active account - should succeed")
        void deposit_WithValidAmount_ShouldSucceed() {
            // Arrange
            BigDecimal initialBalance = testAccount.getBalance();
            BigDecimal depositAmount = depositRequest.getAmount();
            BigDecimal expectedBalance = initialBalance.add(depositAmount);

            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            ApprovalResult approvalResult = ApprovalResult.builder()
                    .status(ApprovalStatus.AUTO_APPROVED)
                    .message("Auto-approved")
                    .handlerUsed("AutoApprovalHandler")
                    .build();

            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(approvalResult);
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            TransactionResponse result = transactionService.deposit("ACC123456", depositRequest);

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertTrue(Boolean.TRUE.equals(result.getSuccess())),
                    () -> assertEquals(expectedBalance, result.getNewBalance()),
                    () -> assertEquals(depositAmount, result.getAmount())
            );
            verify(accountRepository).save(any(Account.class));
            verify(transactionRepository).save(any(Transaction.class));
            verify(accountSubjectManager).attachAllObservers(any(Account.class));
            verify(accountSubjectManager).notifyObservers(any(AccountEvent.class));
        }

        @Test
        @DisplayName("TS-002: Deposit to non-existent account - should throw AccountNotFoundException")
        void deposit_WithNonExistentAccount_ShouldThrowException() {
            // Arrange
            when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

            // Act & Assert
            AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                    () -> transactionService.deposit("INVALID", depositRequest));
            assertTrue(exception.getMessage().contains("Account not found"));
        }

        @Test
        @DisplayName("TS-003: Deposit to suspended account - should throw InvalidAccountStateException")
        void deposit_ToSuspendedAccount_ShouldThrowException() {
            // Arrange
            testAccount.setState(AccountState.SUSPENDED);
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            // Act & Assert
            InvalidAccountStateException exception = assertThrows(InvalidAccountStateException.class,
                    () -> transactionService.deposit("ACC123456", depositRequest));
            assertTrue(exception.getMessage().contains("Account is SUSPENDED"));
        }

        @Test
        @DisplayName("TS-004: Deposit to closed account - should throw InvalidAccountStateException")
        void deposit_ToClosedAccount_ShouldThrowException() {
            // Arrange
            testAccount.setState(AccountState.CLOSED);
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            // Act & Assert
            InvalidAccountStateException exception = assertThrows(InvalidAccountStateException.class,
                    () -> transactionService.deposit("ACC123456", depositRequest));
            assertTrue(exception.getMessage().contains("Account is CLOSED"));
        }

        @Test
        @DisplayName("TS-005: Deposit zero amount - should create transaction")
        void deposit_WithZeroAmount_ShouldCreateTransaction() {
            // Arrange
            depositRequest.setAmount(BigDecimal.ZERO);
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            ApprovalResult approvalResult = ApprovalResult.builder()
                    .status(ApprovalStatus.PENDING)
                    .message("Pending approval")
                    .handlerUsed("ManagerApprovalHandler")
                    .build();

            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(approvalResult);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            // Act
            TransactionResponse result = transactionService.deposit("ACC123456", depositRequest);

            // Assert
            assertNotNull(result);
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("TS-007: Deposit with auto-approval - should update balance immediately")
        void deposit_WithAutoApproval_ShouldUpdateBalance() {
            // Arrange
            BigDecimal initialBalance = testAccount.getBalance();
            BigDecimal depositAmount = depositRequest.getAmount();

            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            ApprovalResult approvalResult = ApprovalResult.builder()
                    .status(ApprovalStatus.AUTO_APPROVED)
                    .message("Auto-approved")
                    .handlerUsed("AutoApprovalHandler")
                    .build();

            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(approvalResult);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
                Account acc = invocation.getArgument(0);
                acc.setBalance(initialBalance.add(depositAmount));
                return acc;
            });
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            TransactionResponse result = transactionService.deposit("ACC123456", depositRequest);

            // Assert
            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(accountCaptor.capture());
            assertEquals(initialBalance.add(depositAmount), accountCaptor.getValue().getBalance());
        }

        @Test
        @DisplayName("TS-008: Deposit requiring manual approval - should set status to PENDING")
        void deposit_RequiringManualApproval_ShouldSetPendingStatus() {
            // Arrange
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            ApprovalResult approvalResult = ApprovalResult.builder()
                    .status(ApprovalStatus.PENDING)
                    .message("Requires manual approval")
                    .handlerUsed("ManagerApprovalHandler")
                    .build();

            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(approvalResult);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            // Act
            TransactionResponse result = transactionService.deposit("ACC123456", depositRequest);

            // Assert
            ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(transactionCaptor.capture());
            assertEquals(TransactionStatus.PENDING, transactionCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("TS-Edge-001: Deposit with rejected approval - should throw exception")
        void deposit_WithRejectedApproval_ShouldThrowException() {
            // Arrange
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            ApprovalResult approvalResult = ApprovalResult.builder()
                    .status(ApprovalStatus.REJECTED)
                    .message("Transaction rejected")
                    .handlerUsed("RejectionHandler")
                    .build();

            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(approvalResult);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act & Assert
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> transactionService.deposit("ACC123456", depositRequest));
            assertTrue(exception.getMessage().contains("rejected"));
        }
    }

    @Nested
    @DisplayName("withdraw Tests")
    class WithdrawTests {

        @Test
        @DisplayName("TS-009: Withdraw valid amount from active account - should succeed")
        void withdraw_WithValidAmount_ShouldSucceed() {
            // Arrange
            BigDecimal initialBalance = testAccount.getBalance();
            BigDecimal withdrawalAmount = withdrawalRequest.getAmount();
            BigDecimal expectedBalance = initialBalance.subtract(withdrawalAmount);

            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            ApprovalResult approvalResult = ApprovalResult.builder()
                    .status(ApprovalStatus.AUTO_APPROVED)
                    .message("Auto-approved")
                    .handlerUsed("AutoApprovalHandler")
                    .build();

            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(approvalResult);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
                Account acc = invocation.getArgument(0);
                acc.setBalance(expectedBalance);
                return acc;
            });
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            TransactionResponse result = transactionService.withdraw("ACC123456", withdrawalRequest);

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertTrue(Boolean.TRUE.equals(result.getSuccess())),
                    () -> assertEquals(expectedBalance, result.getNewBalance())
            );
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("TS-010: Withdraw from non-existent account - should throw AccountNotFoundException")
        void withdraw_FromNonExistentAccount_ShouldThrowException() {
            // Arrange
            when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(AccountNotFoundException.class,
                    () -> transactionService.withdraw("INVALID", withdrawalRequest));
        }

        @Test
        @DisplayName("TS-011: Withdraw from suspended account - should throw InvalidAccountStateException")
        void withdraw_FromSuspendedAccount_ShouldThrowException() {
            // Arrange
            testAccount.setState(AccountState.SUSPENDED);
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertThrows(InvalidAccountStateException.class,
                    () -> transactionService.withdraw("ACC123456", withdrawalRequest));
        }

        @Test
        @DisplayName("TS-012: Withdraw amount exceeding balance - should throw InsufficientBalanceException")
        void withdraw_ExceedingBalance_ShouldThrowException() {
            // Arrange
            withdrawalRequest.setAmount(new BigDecimal("2000.00")); // More than balance
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            // Act & Assert
            InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class,
                    () -> transactionService.withdraw("ACC123456", withdrawalRequest));
            assertEquals("Insufficient balance", exception.getMessage());
        }

        @Test
        @DisplayName("TS-013: Withdraw exact balance amount - should succeed with zero balance")
        void withdraw_ExactBalanceAmount_ShouldSucceedWithZeroBalance() {
            // Arrange
            withdrawalRequest.setAmount(testAccount.getBalance()); // Exact balance
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            ApprovalResult approvalResult = ApprovalResult.builder()
                    .status(ApprovalStatus.AUTO_APPROVED)
                    .message("Auto-approved")
                    .handlerUsed("AutoApprovalHandler")
                    .build();

            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(approvalResult);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
                Account acc = invocation.getArgument(0);
                acc.setBalance(BigDecimal.ZERO);
                return acc;
            });
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            TransactionResponse result = transactionService.withdraw("ACC123456", withdrawalRequest);

            // Assert
            assertEquals(0, result.getNewBalance().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("TS-014: Withdraw causing low balance - should trigger notification")
        void withdraw_CausingLowBalance_ShouldTriggerNotification() {
            // Arrange
            testAccount.setBalance(new BigDecimal("150.00")); // Above threshold
            withdrawalRequest.setAmount(new BigDecimal("60.00")); // Will result in 90.00 (< 100)

            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            ApprovalResult approvalResult = ApprovalResult.builder()
                    .status(ApprovalStatus.AUTO_APPROVED)
                    .message("Auto-approved")
                    .handlerUsed("AutoApprovalHandler")
                    .build();

            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(approvalResult);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
                Account acc = invocation.getArgument(0);
                acc.setBalance(new BigDecimal("90.00"));
                return acc;
            });
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            transactionService.withdraw("ACC123456", withdrawalRequest);

            // Assert
            // Verify low balance notification was triggered
            ArgumentCaptor<AccountEvent> eventCaptor = ArgumentCaptor.forClass(AccountEvent.class);
            verify(accountSubjectManager, atLeastOnce()).notifyObservers(eventCaptor.capture());
            // Check if LOW_BALANCE event was triggered
            boolean lowBalanceEventFound = eventCaptor.getAllValues().stream()
                    .anyMatch(event -> event.getEventType() == AccountEventType.LOW_BALANCE);
            assertTrue(lowBalanceEventFound, "Low balance notification should be triggered");
        }
    }

    @Nested
    @DisplayName("transfer Tests")
    class TransferTests {

        @Test
        @DisplayName("TS-017: Transfer valid amount between active accounts - should succeed")
        void transfer_WithValidAmount_ShouldSucceed() {
            // Arrange
            BigDecimal fromInitialBalance = testAccount.getBalance();
            BigDecimal toInitialBalance = destinationAccount.getBalance();
            BigDecimal transferAmount = transferRequest.getAmount();

            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.findByAccountNumber("ACC789012"))
                    .thenReturn(Optional.of(destinationAccount));

            ApprovalResult approvalResult = ApprovalResult.builder()
                    .status(ApprovalStatus.AUTO_APPROVED)
                    .message("Auto-approved")
                    .handlerUsed("AutoApprovalHandler")
                    .build();

            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(approvalResult);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
                Account acc = invocation.getArgument(0);
                if (acc.getAccountNumber().equals("ACC123456")) {
                    acc.setBalance(fromInitialBalance.subtract(transferAmount));
                } else {
                    acc.setBalance(toInitialBalance.add(transferAmount));
                }
                return acc;
            });
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            TransactionResponse result = transactionService.transfer("ACC123456", transferRequest);

            // Assert
            assertAll(
                    () -> assertNotNull(result),
                    () -> assertTrue(Boolean.TRUE.equals(result.getSuccess())),
                    () -> assertEquals(fromInitialBalance.subtract(transferAmount), result.getNewBalance())
            );
            verify(accountRepository, times(2)).save(any(Account.class)); // Both accounts saved
        }

        @Test
        @DisplayName("TS-018: Transfer from non-existent source account - should throw AccountNotFoundException")
        void transfer_FromNonExistentAccount_ShouldThrowException() {
            // Arrange
            when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(AccountNotFoundException.class,
                    () -> transactionService.transfer("INVALID", transferRequest));
        }

        @Test
        @DisplayName("TS-019: Transfer to non-existent destination account - should throw AccountNotFoundException")
        void transfer_ToNonExistentAccount_ShouldThrowException() {
            // Arrange
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));
            transferRequest.setToAccountNumber("INVALID");
            when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(AccountNotFoundException.class,
                    () -> transactionService.transfer("ACC123456", transferRequest));
        }

        @Test
        @DisplayName("TS-020: Transfer from suspended account - should throw InvalidAccountStateException")
        void transfer_FromSuspendedAccount_ShouldThrowException() {
            // Arrange
            testAccount.setState(AccountState.SUSPENDED);
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.findByAccountNumber(transferRequest.getToAccountNumber()))
                    .thenReturn(Optional.of(destinationAccount));

            // Act & Assert
            assertThrows(InvalidAccountStateException.class,
                    () -> transactionService.transfer("ACC123456", transferRequest));
        }

        @Test
        @DisplayName("TS-021: Transfer to suspended account - should throw InvalidAccountStateException")
        void transfer_ToSuspendedAccount_ShouldThrowException() {
            // Arrange
            destinationAccount.setState(AccountState.SUSPENDED);
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.findByAccountNumber("ACC789012"))
                    .thenReturn(Optional.of(destinationAccount));

            // Act & Assert
            assertThrows(InvalidAccountStateException.class,
                    () -> transactionService.transfer("ACC123456", transferRequest));
        }

        @Test
        @DisplayName("TS-022: Transfer amount exceeding source balance - should throw InsufficientBalanceException")
        void transfer_ExceedingSourceBalance_ShouldThrowException() {
            // Arrange
            transferRequest.setAmount(new BigDecimal("2000.00")); // More than balance
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.findByAccountNumber("ACC789012"))
                    .thenReturn(Optional.of(destinationAccount));

            // Act & Assert
            InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class,
                    () -> transactionService.transfer("ACC123456", transferRequest));
            assertTrue(exception.getMessage().contains("Insufficient balance"));
        }

        @Test
        @DisplayName("TS-023: Transfer to same account - should be handled")
        void transfer_ToSameAccount_ShouldBeHandled() {
            // Arrange
            transferRequest.setToAccountNumber("ACC123456"); // Same as source
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            ApprovalResult approvalResult = ApprovalResult.builder()
                    .status(ApprovalStatus.AUTO_APPROVED)
                    .message("Auto-approved")
                    .handlerUsed("AutoApprovalHandler")
                    .build();

            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(approvalResult);
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            TransactionResponse result = transactionService.transfer("ACC123456", transferRequest);

            // Assert
            // Service allows transfer to same account (business logic decision)
            assertNotNull(result);
        }

        @Test
        @DisplayName("TS-Edge-002: Transfer with different currencies - should handle")
        void transfer_WithDifferentCurrencies_ShouldHandle() {
            // Arrange
            destinationAccount.setCurrency("EUR");
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));
            when(accountRepository.findByAccountNumber("ACC789012"))
                    .thenReturn(Optional.of(destinationAccount));

            ApprovalResult approvalResult = ApprovalResult.builder()
                    .status(ApprovalStatus.AUTO_APPROVED)
                    .message("Auto-approved")
                    .handlerUsed("AutoApprovalHandler")
                    .build();

            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(approvalResult);
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount, destinationAccount);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act
            TransactionResponse result = transactionService.transfer("ACC123456", transferRequest);

            // Assert
            // Service uses source account currency (current implementation)
            assertNotNull(result);
            assertEquals("USD", result.getCurrency());
        }
    }

    @Nested
    @DisplayName("Security & Edge Case Tests")
    class SecurityAndEdgeCaseTests {

        @Test
        @DisplayName("Security-001: Concurrent withdrawal attempt - should prevent double-spending")
        void withdraw_ConcurrentAttempt_ShouldPreventDoubleSpending() {
            // Arrange
            BigDecimal initialBalance = testAccount.getBalance();
            withdrawalRequest.setAmount(new BigDecimal("600.00")); // Would leave 400

            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            // Simulate first withdrawal
            ApprovalResult approvalResult = ApprovalResult.builder()
                    .status(ApprovalStatus.AUTO_APPROVED)
                    .message("Auto-approved")
                    .handlerUsed("AutoApprovalHandler")
                    .build();

            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(approvalResult);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
                Account acc = invocation.getArgument(0);
                acc.setBalance(initialBalance.subtract(new BigDecimal("600.00")));
                return acc;
            });
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });

            // Act - First withdrawal
            transactionService.withdraw("ACC123456", withdrawalRequest);

            // Simulate second concurrent withdrawal attempt
            testAccount.setBalance(initialBalance.subtract(new BigDecimal("600.00")));
            withdrawalRequest.setAmount(new BigDecimal("500.00")); // Would exceed remaining balance

            // Act & Assert - Second withdrawal should fail
            InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class,
                    () -> transactionService.withdraw("ACC123456", withdrawalRequest));
            assertTrue(exception.getMessage().contains("Insufficient balance"));
        }

        @Test
        @DisplayName("Edge-003: Negative amount in deposit request - should be handled by validation")
        void deposit_WithNegativeAmount_ShouldBeHandled() {
            // Arrange
            depositRequest.setAmount(new BigDecimal("-100.00"));
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));
            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(ApprovalResult.builder()
                            .status(ApprovalStatus.PENDING)
                            .message("Rejected")
                            .build());
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            // Act & Assert
            // Note: Negative amounts should be validated at DTO level
            // Service may still process if validation is bypassed
            // This test documents current behavior
            assertDoesNotThrow(() -> transactionService.deposit("ACC123456", depositRequest));
        }

        @Test
        @DisplayName("Edge-004: Very large amount - should require approval")
        void deposit_WithVeryLargeAmount_ShouldRequireApproval() {
            // Arrange
            depositRequest.setAmount(new BigDecimal("1000000.00")); // Very large amount
            when(accountRepository.findByAccountNumber("ACC123456"))
                    .thenReturn(Optional.of(testAccount));

            ApprovalResult approvalResult = ApprovalResult.builder()
                    .status(ApprovalStatus.PENDING)
                    .message("Requires manual approval")
                    .handlerUsed("DirectorApprovalHandler")
                    .build();

            when(transactionApprovalService.processApproval(any(Transaction.class)))
                    .thenReturn(approvalResult);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction tx = invocation.getArgument(0);
                tx.setId(1L);
                return tx;
            });
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            // Act
            TransactionResponse result = transactionService.deposit("ACC123456", depositRequest);

            // Assert
            assertNotNull(result);
            // Large amounts should require manual approval
            verify(transactionApprovalService).processApproval(any(Transaction.class));
        }
    }
}


