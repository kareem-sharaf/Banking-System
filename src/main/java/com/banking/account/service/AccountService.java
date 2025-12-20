package com.banking.account.service;

import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.dto.UpdateAccountRequest;
import com.banking.account.dto.AccountResponse;
import com.banking.account.module.entity.Account;
import com.banking.account.module.entity.AccountType;
import com.banking.customer.module.entity.Customer;
import com.banking.core.enums.AccountState;
import com.banking.core.exception.AccountNotFoundException;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.AccountTypeRepository;
import com.banking.customer.repository.CustomerRepository;
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
 * Service layer for Account Management (CRUD).
 * Transaction operations are now handled by TransactionService.
 * 
 * @author Banking System
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final CustomerRepository customerRepository;

    /**
     * Create a new account
     */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        logger.info("Creating new account for customer: {}", request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        AccountType accountType = accountTypeRepository.findByCode(request.getAccountTypeCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid account type code"));

        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setCustomer(customer);
        account.setAccountType(accountType);
        account.setBalance(request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO);
        account.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        account.setState(AccountState.ACTIVE);
        account.setOpenedDate(LocalDateTime.now());
        
        account = accountRepository.save(account);

        return mapToResponse(account);
    }

    /**
     * Get account details
     */
    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return mapToResponse(account);
    }

    /**
     * Update account details
     */
    @Transactional
    public AccountResponse updateAccount(String accountNumber, UpdateAccountRequest request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (request.getState() != null) {
            account.setState(request.getState());
        }

        // Logic for upgrading account type could go here

        account = accountRepository.save(account);
        return mapToResponse(account);
    }

    /**
     * Close account
     */
    @Transactional
    public void closeAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot close account with positive balance");
        }

        account.setState(AccountState.CLOSED);
        account.setClosedDate(LocalDateTime.now());
        accountRepository.save(account);
    }

    /**
     * Link accounts (Parent/Child)
     */
    @Transactional
    public void linkAccounts(String parentAccountNumber, String childAccountNumber) {
        Account parent = accountRepository.findByAccountNumber(parentAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Parent account not found"));
        Account child = accountRepository.findByAccountNumber(childAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Child account not found"));

        child.setParentAccount(parent);
        accountRepository.save(child);
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType().getName())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .state(account.getState())
                .createdAt(account.getCreatedAt())
                .customerId(account.getCustomer().getId())
                .build();
    }

    private String generateAccountNumber() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
