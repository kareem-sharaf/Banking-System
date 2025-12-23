package com.banking.transaction.service;

import com.banking.account.dto.DepositRequest;
import com.banking.account.dto.WithdrawalRequest;
import com.banking.account.module.entity.Account;
import com.banking.account.repository.AccountRepository;
import com.banking.account.service.notification.AccountSubjectManager;
import com.banking.core.enums.AccountState;
import com.banking.transaction.approval.ApprovalResult;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.module.entity.Transaction;
import com.banking.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Security-focused tests for negative amount handling in TransactionService.
 *
 * These tests are expected to FAIL if the service does not validate negative amounts.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class TransactionServiceNegativeAmountSecurityTest {

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

    private Account account;
    private Account toAccount;

    @BeforeEach
    void setup() {
        account = new Account();
        account.setId(1L);
        account.setAccountNumber("ACC123");
        account.setBalance(new BigDecimal("1000.00"));
        account.setState(AccountState.ACTIVE);
        account.setCurrency("USD");
        account.setOpenedDate(LocalDateTime.now());

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setAccountNumber("ACC999");
        toAccount.setBalance(new BigDecimal("500.00"));
        toAccount.setState(AccountState.ACTIVE);
        toAccount.setCurrency("USD");
        toAccount.setOpenedDate(LocalDateTime.now());

        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountNumber("ACC999")).thenReturn(Optional.of(toAccount));
        // Default approval result to avoid NPE paths; service under test should still reject negative.
        when(transactionApprovalService.processApproval(any(Transaction.class)))
                .thenReturn(ApprovalResult.builder().build());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("Negative amount security cases")
    class NegativeAmountCases {

        @Test
        @DisplayName("deposit(-100) should be rejected")
        void deposit_NegativeAmount_ShouldThrow() {
            DepositRequest req = new DepositRequest();
            req.setAmount(new BigDecimal("-100"));

            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.deposit("ACC123", req));
        }

        @Test
        @DisplayName("withdraw(-50) should be rejected")
        void withdraw_NegativeAmount_ShouldThrow() {
            WithdrawalRequest req = new WithdrawalRequest();
            req.setAmount(new BigDecimal("-50"));

            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.withdraw("ACC123", req));
        }

        @Test
        @DisplayName("transfer(-200) should be rejected")
        void transfer_NegativeAmount_ShouldThrow() {
            TransferRequest req = new TransferRequest();
            req.setToAccountNumber("ACC999");
            req.setAmount(new BigDecimal("-200"));

            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.transfer("ACC123", req));
        }
    }
}

