package com.banking.observer.impl;

import com.banking.account.module.entity.Account;
import com.banking.core.notification.module.entity.Notification;
import com.banking.core.auth.module.entity.User;
import com.banking.core.enums.AccountEventType;
import com.banking.core.enums.NotificationPriority;
import com.banking.core.enums.NotificationType;
import com.banking.observer.AccountObserver;
import com.banking.observer.event.AccountEvent;
import com.banking.core.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class InAppNotifier implements AccountObserver {

    private static final Logger logger = LoggerFactory.getLogger(InAppNotifier.class);

    private final NotificationRepository notificationRepository;

    @Override
    public void update(AccountEvent event) {
        try {
            Account account = event.getAccount();
            if (account == null || account.getCustomer() == null) {
                logger.warn("InAppNotifier: Account or customer is null, skipping notification");
                return;
            }

            User user = account.getCustomer().getUser();
            if (user == null) {
                logger.warn("InAppNotifier: User is null, skipping notification");
                return;
            }

            // Determine notification type and priority
            NotificationType notificationType = determineNotificationType(event.getEventType());
            NotificationPriority priority = determinePriority(event.getEventType());

            // Create notification
            String title = buildNotificationTitle(event);
            String message = buildNotificationMessage(event);

            Notification notification = new Notification();
            notification.setUserId(user.getId());
            notification.setAccount(account);
            notification.setNotificationType(notificationType);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setPriority(priority);
            notification.setIsRead(false);
            notification.setCreatedDate(LocalDateTime.now());

            notificationRepository.save(notification);

            logger.debug("In-app notification created for user {} - Event: {}",
                    user.getId(), event.getEventType());

        } catch (Exception e) {
            logger.error("Error creating in-app notification for event: {}", event.getEventType(), e);
        }
    }

    @Override
    public String getObserverType() {
        return "IN_APP";
    }

    /**
     * Determine notification type based on event type
     */
    private NotificationType determineNotificationType(AccountEventType eventType) {
        return switch (eventType) {
            case DEPOSIT_COMPLETED, WITHDRAWAL_COMPLETED, TRANSFER_COMPLETED -> NotificationType.BALANCE_CHANGE;
            case LOW_BALANCE -> NotificationType.ACCOUNT_ALERT;
            case SUSPICIOUS_ACTIVITY -> NotificationType.ACCOUNT_ALERT;
            default -> NotificationType.SYSTEM_NOTIFICATION;
        };
    }

    /**
     * Determine notification priority based on event type
     */
    private NotificationPriority determinePriority(AccountEventType eventType) {
        return switch (eventType) {
            case SUSPICIOUS_ACTIVITY -> NotificationPriority.URGENT;
            case LOW_BALANCE -> NotificationPriority.HIGH;
            case DEPOSIT_COMPLETED, WITHDRAWAL_COMPLETED, TRANSFER_COMPLETED -> NotificationPriority.MEDIUM;
            default -> NotificationPriority.LOW;
        };
    }

    /**
     * Build notification title
     */
    private String buildNotificationTitle(AccountEvent event) {
        return switch (event.getEventType()) {
            case DEPOSIT_COMPLETED -> "Deposit Completed";
            case WITHDRAWAL_COMPLETED -> "Withdrawal Completed";
            case TRANSFER_COMPLETED -> "Transfer Completed";
            case LOW_BALANCE -> "Low Balance Alert";
            case SUSPICIOUS_ACTIVITY -> "Security Alert";
            default -> "Account Activity";
        };
    }

    /**
     * Build notification message
     */
    private String buildNotificationMessage(AccountEvent event) {
        Account account = event.getAccount();
        StringBuilder message = new StringBuilder();

        switch (event.getEventType()) {
            case DEPOSIT_COMPLETED:
                message.append(String.format("Deposit of %s %s completed to account %s.",
                        event.getAmount(), account.getCurrency(), account.getAccountNumber()));
                break;

            case WITHDRAWAL_COMPLETED:
                message.append(String.format("Withdrawal of %s %s completed from account %s.",
                        event.getAmount(), account.getCurrency(), account.getAccountNumber()));
                break;

            case TRANSFER_COMPLETED:
                message.append(String.format("Transfer of %s %s completed.",
                        event.getAmount(), account.getCurrency()));
                break;

            case LOW_BALANCE:
                message.append(String.format("Your account %s balance is low: %s %s.",
                        account.getAccountNumber(), event.getNewBalance(), account.getCurrency()));
                break;

            case SUSPICIOUS_ACTIVITY:
                message.append(String.format("Unusual activity detected on account %s. Please review.",
                        account.getAccountNumber()));
                break;

            default:
                message.append(String.format("Activity on account %s: %s",
                        account.getAccountNumber(), event.getEventType()));
        }

        if (event.getNewBalance() != null &&
                (event.getEventType() == AccountEventType.DEPOSIT_COMPLETED ||
                        event.getEventType() == AccountEventType.WITHDRAWAL_COMPLETED ||
                        event.getEventType() == AccountEventType.TRANSFER_COMPLETED)) {
            message.append(String.format(" New balance: %s %s.",
                    event.getNewBalance(), account.getCurrency()));
        }

        if (event.getTransaction() != null && event.getTransaction().getTransactionNumber() != null) {
            message.append(String.format(" Transaction #%s.",
                    event.getTransaction().getTransactionNumber()));
        }

        return message.toString();
    }
}
