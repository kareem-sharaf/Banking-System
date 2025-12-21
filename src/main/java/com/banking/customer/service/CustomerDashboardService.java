package com.banking.customer.service;

import com.banking.account.module.entity.Account;
import com.banking.core.notification.module.entity.Notification;
import com.banking.customer.module.entity.Customer;
import com.banking.transaction.module.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Customer Dashboard Service
 *
 * Contains business logic for dashboard operations.
 * Orchestrates calls to query providers to fetch and prepare dashboard data.
 * Handles domain-specific logic while delegating data access to query providers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerDashboardService {

    private final com.banking.customer.extra.provider.CustomerQueryProvider customerQueryProvider;
    private final com.banking.account.extra.provider.AccountQueryProvider accountQueryProvider;
    private final com.banking.transaction.extra.provider.TransactionQueryProvider transactionQueryProvider;
    private final com.banking.core.notification.extra.provider.NotificationQueryProvider notificationQueryProvider;

    /**
     * Get customer dashboard data
     *
     * @param username the username to fetch dashboard data for
     * @return CustomerDashboardData containing all dashboard entities
     */
    @Transactional(readOnly = true)
    public CustomerDashboardData getCustomerDashboardData(String username) {
        log.debug("Service: Fetching dashboard data for user: {}", username);

        // 1. Get customer with user data
        Customer customer = customerQueryProvider.findCustomerWithUserByUsername(username);

        // 2. Get accounts for the customer
        List<Account> accounts = accountQueryProvider.findAccountsByCustomerId(customer.getId());

        // 3. Get recent transactions for all customer accounts (optimized batch query)
        List<Long> accountIds = accounts.stream().map(Account::getId).toList();
        List<Transaction> recentTransactions = accountIds.isEmpty()
            ? List.of()
            : transactionQueryProvider.findRecentTransactionsByAccountIds(accountIds, 10);

        // 4. Get unread notifications for the user
        List<Notification> unreadNotifications = notificationQueryProvider
            .findUnreadNotificationsByUserId(customer.getUser().getId());

        log.debug("Service: Dashboard data retrieved - customer: {}, accounts: {}, transactions: {}, notifications: {}",
                 customer.getCustomerNumber(), accounts.size(), recentTransactions.size(), unreadNotifications.size());

        return CustomerDashboardData.builder()
                .customer(customer)
                .accounts(accounts)
                .recentTransactions(recentTransactions)
                .unreadNotifications(unreadNotifications)
                .build();
    }

    /**
     * Data transfer object containing all dashboard entities
     */
    @lombok.Builder
    @lombok.Data
    public static class CustomerDashboardData {
        private final Customer customer;
        private final List<Account> accounts;
        private final List<Transaction> recentTransactions;
        private final List<Notification> unreadNotifications;
    }
}
