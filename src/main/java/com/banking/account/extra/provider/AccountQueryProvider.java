package com.banking.account.extra.provider;

import com.banking.account.module.entity.Account;
import com.banking.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Account Query Provider
 *
 * Encapsulates read-only queries for Account domain entities.
 * Provides optimized access to account-related data without business logic.
 * Returns domain entities for use by facades and other query providers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountQueryProvider {

    private final AccountRepository accountRepository;

    /**
     * Find all accounts for a customer
     *
     * @param customerId the customer ID to search for
     * @return List of Account entities belonging to the customer
     */
    public List<Account> findAccountsByCustomerId(Long customerId) {
        log.debug("Finding accounts for customer ID: {}", customerId);
        return accountRepository.findByCustomerId(customerId);
    }

    /**
     * Find account by account number
     *
     * @param accountNumber the account number to search for
     * @return Account entity if found
     */
    public Account findAccountByAccountNumber(String accountNumber) {
        log.debug("Finding account by account number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .orElse(null);
    }

    /**
     * Check if account exists by account number
     *
     * @param accountNumber the account number to check
     * @return true if account exists, false otherwise
     */
    public boolean accountExistsByAccountNumber(String accountNumber) {
        return accountRepository.existsByAccountNumber(accountNumber);
    }

    /**
     * Find accounts by account type and state
     *
     * @param accountTypeId the account type ID
     * @param state the account state
     * @return List of Account entities matching the criteria
     */
    public List<Account> findAccountsByTypeAndState(Long accountTypeId, com.banking.core.enums.AccountState state) {
        log.debug("Finding accounts by type ID: {} and state: {}", accountTypeId, state);
        return accountRepository.findByAccountTypeIdAndState(accountTypeId, state);
    }
}
