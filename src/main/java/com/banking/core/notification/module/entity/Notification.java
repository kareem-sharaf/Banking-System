package com.banking.core.notification.module.entity;

import com.banking.core.enums.NotificationType;
import com.banking.core.enums.NotificationPriority;
import com.banking.account.module.entity.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Notification Entity
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_is_read", columnList = "is_read"),
        @Index(name = "idx_created_date", columnList = "created_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "read_date")
    private LocalDateTime readDate;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPriority priority = NotificationPriority.MEDIUM;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
}
