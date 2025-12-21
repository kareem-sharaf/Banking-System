package com.banking.customer.facade.impl;

import com.banking.account.module.entity.Account;
import com.banking.account.repository.AccountRepository;
import com.banking.core.notification.module.entity.Notification;
import com.banking.core.notification.repository.NotificationRepository;
import com.banking.customer.module.entity.Customer;
import com.banking.customer.repository.CustomerRepository;
import com.banking.customer.facade.CustomerDashboardFacade;
import com.banking.customer.facade.dto.AccountSummaryDto;
import com.banking.customer.facade.dto.CustomerDashboardDto;
import com.banking.customer.facade.dto.NotificationSummaryDto;
import com.banking.customer.facade.dto.TransactionSummaryDto;
import com.banking.transaction.module.entity.Transaction;
import com.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerDashboardFacadeImpl implements CustomerDashboardFacade {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomerDashboardDto getCustomerDashboard(String username) {
        
        // 1. Fetch Customer
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found for user: " + username));

        // 2. Fetch Accounts
        List<Account> accounts = accountRepository.findByCustomerId(customer.getId());
        
        // 3. Fetch Transactions (last 10 recent across all accounts)
        List<Long> accountIds = accounts.stream().map(Account::getId).toList();
        List<Transaction> allTransactions = new ArrayList<>();
        
        // Note: Ideally efficient query like findAllByAccountIdInOrderByDateDesc
        for (Long accountId : accountIds) {
            allTransactions.addAll(transactionRepository.findAllByAccountId(accountId));
        }

        List<Transaction> recentTransactions = allTransactions.stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDate).reversed())
                .limit(10)
                .toList();

        // 4. Fetch Notifications (Unread)
        // Assuming we can find by userId. Customer has a User associated.
        List<Notification> notifications = notificationRepository.findByUserIdAndIsRead(customer.getUser().getId(), false);


        // 5. Assemble DTO
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
                .read(notif.isRead())
                .build();
    }
}
