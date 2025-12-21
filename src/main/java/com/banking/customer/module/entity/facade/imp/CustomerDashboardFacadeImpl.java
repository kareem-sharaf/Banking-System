package com.banking.customer.module.entity.facade.imp;

import com.banking.account.dto.AccountSummaryDto;
import com.banking.account.module.entity.Account;
import com.banking.core.notification.dto.NotificationSummaryDto;
import com.banking.core.notification.module.entity.Notification;
import com.banking.customer.module.entity.Customer;
import com.banking.customer.module.entity.facade.CustomerDashboardFacade;
import com.banking.customer.dto.CustomerDashboardDto;
import com.banking.customer.service.CustomerDashboardService;
import com.banking.transaction.dto.TransactionSummaryDto;
import com.banking.transaction.module.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Customer Dashboard Facade Implementation
 *
 * Implements the Facade pattern to provide a simplified interface for aggregating
 * dashboard data from multiple modules through the service layer.
 *
 * Architecture layers:
 * - Facade: Orchestrates cross-module calls and assembles final DTO
 * - Service: Contains business logic and coordinates query providers
 * - Query Providers: Encapsulate optimized repository access per module
 * - Repositories: Handle raw database operations
 *
 * Benefits:
 * - Clean separation of concerns across architectural layers
 * - Service layer contains domain-specific business logic
 * - Query providers optimize data access patterns
 * - Facade focuses solely on DTO assembly and cross-module orchestration
 * - Each layer is independently testable and maintainable
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerDashboardFacadeImpl implements CustomerDashboardFacade {

    private final CustomerDashboardService customerDashboardService;

    @Override
    @Transactional(readOnly = true)
    public CustomerDashboardDto getCustomerDashboard(String username) {
        log.debug("Fetching dashboard data for user: {}", username);

        // 1. Get all dashboard data from service
        CustomerDashboardService.CustomerDashboardData dashboardData =
            customerDashboardService.getCustomerDashboardData(username);

        // 2. Extract entities from service response
        Customer customer = dashboardData.getCustomer();
        List<Account> accounts = dashboardData.getAccounts();
        List<Transaction> recentTransactions = dashboardData.getRecentTransactions();
        List<Notification> notifications = dashboardData.getUnreadNotifications();

        log.debug("Dashboard data fetched: {} accounts, {} transactions, {} notifications",
                  accounts.size(), recentTransactions.size(), notifications.size());

        // 3. Assemble DTO
        return CustomerDashboardDto.builder()
                .customerName(customer.getUser().getFirstName() + " " + customer.getUser().getLastName())
                .customerNumber(customer.getCustomerNumber())
                .customerStatus(customer.getStatus().name())
                .accounts(accounts.stream().map(this::mapAccount).collect(Collectors.toList()))
                .recentTransactions(recentTransactions.stream().map(this::mapTransaction).collect(Collectors.toList()))
                .notifications(notifications.stream().map(this::mapNotification).collect(Collectors.toList()))
                .build();
    }

    private AccountSummaryDto mapAccount(Account account) {
        return AccountSummaryDto.builder()
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType().getName())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .state(account.getState().name())
                .build();
    }

    private TransactionSummaryDto mapTransaction(Transaction tx) {
        return TransactionSummaryDto.builder()
                .transactionNumber(tx.getTransactionNumber())
                .type(tx.getTransactionType().name())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .date(tx.getTransactionDate())
                .status(tx.getStatus().name())
                .description(tx.getDescription())
                .build();
    }

    private NotificationSummaryDto mapNotification(Notification notif) {
        return NotificationSummaryDto.builder()
                .title(notif.getTitle())
                .message(notif.getMessage())
                .priority(notif.getPriority().name())
                .date(notif.getCreatedDate())
                .read(notif.getIsRead())
                .build();
    }
}

