package com.banking.core.module.entity;

import com.banking.core.enums.ActionType;
import com.banking.transaction.module.entity.Transaction;
import com.banking.account.module.entity.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Audit Log Entity
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_account_id", columnList = "account_id"),
        @Index(name = "idx_action_type", columnList = "action_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 2000)
    private String details;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(length = 50)
    private String status;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
