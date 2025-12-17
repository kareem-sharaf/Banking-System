package com.banking.account.module.entity;

import com.banking.core.enums.AccountState;
import com.banking.customer.module.entity.Customer;
import com.banking.transaction.module.entity.Transaction;
import com.banking.core.notification.module.entity.Notification;
import com.banking.core.module.entity.AuditLog;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Account Entity
 */
@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_account_number", columnList = "account_number"),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_parent_account_id", columnList = "parent_account_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 50)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_type_id", nullable = false)
    private AccountType accountType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private Account parentAccount;

    @OneToMany(mappedBy = "parentAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> childAccounts = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountState state = AccountState.ACTIVE;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "opened_date", nullable = false)
    private LocalDateTime openedDate;

    @Column(name = "closed_date")
    private LocalDateTime closedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "interest_rate", precision = 5, scale = 4)
    private java.math.BigDecimal interestRate;

    @Column(name = "last_interest_calculation")
    private java.time.LocalDate lastInterestCalculation;

    @OneToMany(mappedBy = "fromAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> fromTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "toAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> toTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AuditLog> auditLogs = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InterestCalculation> interestCalculations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (openedDate == null) {
            openedDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
