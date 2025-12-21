package com.banking.customer.module.entity.facade.imp;

import com.banking.account.dto.AccountSummaryDto;
import com.banking.account.module.entity.Account;
import com.banking.account.repository.AccountRepository;
import com.banking.core.exception.CustomerNotFoundException;
import com.banking.core.exception.UserNotFoundException;
import com.banking.core.notification.dto.NotificationSummaryDto;
import com.banking.core.notification.module.entity.Notification;
import com.banking.core.notification.repository.NotificationRepository;
import com.banking.core.auth.module.entity.User;
import com.banking.core.auth.repository.UserRepository;
import com.banking.customer.module.entity.facade.CustomerDashboardFacade;
import com.banking.customer.module.entity.Customer;
import com.banking.customer.repository.CustomerRepository;
import com.banking.customer.dto.CustomerDashboardDto;
import com.banking.transaction.dto.TransactionSummaryDto;
import com.banking.transaction.module.entity.Transaction;
import com.banking.transaction.repository.TransactionRepository;
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
 * dashboard data from multiple services (Account, Transaction, Notification).
 * 
 * This facade hides the complexity of:
 * - Fetching data from multiple repositories
 * - Coordinating between different services
 * - Assembling the final DTO
 * 
 * Benefits:
 * - Single point of access for dashboard data
 * - Reduced coupling between controller and multiple services
 * - Easier to maintain and test
 * - Optimized queries to avoid N+1 problems
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerDashboardFacadeImpl implements CustomerDashboardFacade {

    private static final int RECENT_TRANSACTIONS_LIMIT = 10;

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomerDashboardDto getCustomerDashboard(String username) {
        log.debug("Fetching dashboard data for user: {}", username);
        
        // 1. Fetch User first, then Customer
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        
        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found for user: " + username));

        // 2. Fetch Accounts
        List<Account> accounts = accountRepository.findByCustomerId(customer.getId());
        
        // 3. Fetch Transactions (optimized: single query instead of N+1 queries)
        List<Long> accountIds = accounts.stream().map(Account::getId).toList();
        List<Transaction> recentTransactions = accountIds.isEmpty() 
            ? List.of() 
            : transactionRepository.findRecentTransactionsByAccountIds(accountIds)
                .stream()
                .limit(RECENT_TRANSACTIONS_LIMIT)
                .collect(Collectors.toList());

        // 4. Fetch Notifications (Unread) - use user.getId() to avoid lazy loading
        List<Notification> notifications = notificationRepository.findByUserIdAndIsRead(user.getId(), false);

        log.debug("Dashboard data fetched: {} accounts, {} transactions, {} notifications", 
                  accounts.size(), recentTransactions.size(), notifications.size());

        // 5. Assemble DTO
        return CustomerDashboardDto.builder()
                .customerName(user.getFirstName() + " " + user.getLastName())
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

