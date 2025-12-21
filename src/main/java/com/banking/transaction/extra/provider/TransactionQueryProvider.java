package com.banking.transaction.extra.provider;

import com.banking.transaction.module.entity.Transaction;
import com.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Transaction Query Provider
 *
 * Encapsulates read-only queries for Transaction domain entities.
 * Provides optimized access to transaction-related data without business logic.
 * Returns domain entities for use by facades and other query providers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionQueryProvider {

    private final TransactionRepository transactionRepository;

    /**
     * Find recent transactions for multiple accounts (optimized to avoid N+1 queries)
     *
     * @param accountIds list of account IDs to search for
     * @return List of Transaction entities ordered by date descending
     */
    public List<Transaction> findRecentTransactionsByAccountIds(List<Long> accountIds) {
        log.debug("Finding recent transactions for account IDs: {}", accountIds);
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return transactionRepository.findRecentTransactionsByAccountIds(accountIds);
    }

    /**
     * Find recent transactions for multiple accounts with limit
     *
     * @param accountIds list of account IDs to search for
     * @param limit maximum number of transactions to return
     * @return List of Transaction entities ordered by date descending, limited to specified count
     */
    public List<Transaction> findRecentTransactionsByAccountIds(List<Long> accountIds, int limit) {
        log.debug("Finding recent transactions for account IDs: {} with limit: {}", accountIds, limit);
        return findRecentTransactionsByAccountIds(accountIds).stream()
                .limit(limit)
                .toList();
    }

    /**
     * Find all transactions for a specific account
     *
     * @param accountId the account ID to search for
     * @return List of Transaction entities for the account
     */
    public List<Transaction> findAllTransactionsByAccountId(Long accountId) {
        log.debug("Finding all transactions for account ID: {}", accountId);
        return transactionRepository.findAllByAccountId(accountId);
    }

    /**
     * Find transaction by transaction number
     *
     * @param transactionNumber the transaction number to search for
     * @return Transaction entity if found
     */
    public Transaction findTransactionByTransactionNumber(String transactionNumber) {
        log.debug("Finding transaction by transaction number: {}", transactionNumber);
        return transactionRepository.findByTransactionNumber(transactionNumber)
                .orElse(null);
    }

    /**
     * Check if transaction exists by transaction number
     *
     * @param transactionNumber the transaction number to check
     * @return true if transaction exists, false otherwise
     */
    public boolean transactionExistsByTransactionNumber(String transactionNumber) {
        return transactionRepository.existsByTransactionNumber(transactionNumber);
    }
}
