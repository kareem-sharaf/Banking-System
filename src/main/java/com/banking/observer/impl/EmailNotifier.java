package com.banking.observer.impl;

import com.banking.entity.Account;
import com.banking.entity.Notification;
import com.banking.entity.User;
import com.banking.enums.AccountEventType;
import com.banking.enums.NotificationPriority;
import com.banking.enums.NotificationType;
import com.banking.observer.AccountObserver;
import com.banking.observer.event.AccountEvent;
import com.banking.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmailNotifier implements AccountObserver {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotifier.class);

    private final NotificationRepository notificationRepository;

    @Override
    public void update(AccountEvent event) {
        try {
            Account account = event.getAccount();
            if (account == null || account.getCustomer() == null) {
                logger.warn("EmailNotifier: Account or customer is null, skipping notification");
                return;
            }

            User user = account.getCustomer().getUser();
            if (user == null) {
                logger.warn("EmailNotifier: User is null, skipping notification");
                return;
            }

            // Determine notification priority based on event type
            NotificationPriority priority = determinePriority(event.getEventType());

            // Create notification message
            String title = buildEmailTitle(event);
            String message = buildEmailMessage(event);

            // Create and save notification
            Notification notification = new Notification();
            notification.setUserId(user.getId());
            notification.setAccount(account);
            notification.setNotificationType(NotificationType.SYSTEM_NOTIFICATION);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setPriority(priority);
            notification.setIsRead(false);
            notification.setCreatedDate(LocalDateTime.now());

            notificationRepository.save(notification);

            // Log email notification (in production, this would send actual email)
            logger.info("Email notification sent to user {} for account {} - Event: {}",
                    user.getEmail(), account.getAccountNumber(), event.getEventType());

        } catch (Exception e) {
            logger.error("Error sending email notification for event: {}", event.getEventType(), e);
        }
    }

    @Override
    public String getObserverType() {
        return "EMAIL";
    }

    /**
     * Build email title based on event type
     */
    private String buildEmailTitle(AccountEvent event) {
        return switch (event.getEventType()) {
            case DEPOSIT_COMPLETED -> "Deposit Completed - Account " + event.getAccount().getAccountNumber();
            case WITHDRAWAL_COMPLETED -> "Withdrawal Completed - Account " + event.getAccount().getAccountNumber();
            case TRANSFER_COMPLETED -> "Transfer Completed - Account " + event.getAccount().getAccountNumber();
            case LOW_BALANCE -> "Low Balance Alert - Account " + event.getAccount().getAccountNumber();
            case SUSPICIOUS_ACTIVITY -> "Security Alert - Account " + event.getAccount().getAccountNumber();
            default -> "Account Activity - " + event.getAccount().getAccountNumber();
        };
    }

    /**
     * Build email message based on event
     */
    private String buildEmailMessage(AccountEvent event) {
        Account account = event.getAccount();
        StringBuilder message = new StringBuilder();

        message.append("Dear Customer,\n\n");

        switch (event.getEventType()) {
            case DEPOSIT_COMPLETED:
                message.append(String.format("A deposit of %s %s has been successfully completed to your account %s.\n",
                        event.getAmount(), account.getCurrency(), account.getAccountNumber()));
                if (event.getNewBalance() != null) {
                    message.append(String.format("Your new balance is: %s %s\n",
                            event.getNewBalance(), account.getCurrency()));
                }
                break;

            case WITHDRAWAL_COMPLETED:
                message.append(
                        String.format("A withdrawal of %s %s has been successfully completed from your account %s.\n",
                                event.getAmount(), account.getCurrency(), account.getAccountNumber()));
                if (event.getNewBalance() != null) {
                    message.append(String.format("Your new balance is: %s %s\n",
                            event.getNewBalance(), account.getCurrency()));
                }
                break;

            case TRANSFER_COMPLETED:
                message.append(String.format("A transfer of %s %s has been successfully completed.\n",
                        event.getAmount(), account.getCurrency()));
                if (event.getNewBalance() != null) {
                    message.append(String.format("Your new balance is: %s %s\n",
                            event.getNewBalance(), account.getCurrency()));
                }
                break;

            case LOW_BALANCE:
                message.append(String.format("Your account %s balance is low: %s %s\n",
                        account.getAccountNumber(), event.getNewBalance(), account.getCurrency()));
                message.append("Please consider adding funds to avoid service charges.\n");
                break;

            case SUSPICIOUS_ACTIVITY:
                message.append(String.format("We detected unusual activity on your account %s.\n",
                        account.getAccountNumber()));
                message.append("If this was not you, please contact us immediately.\n");
                break;

            default:
                message.append(String.format("Activity on your account %s: %s\n",
                        account.getAccountNumber(), event.getEventType()));
        }

        if (event.getTransaction() != null && event.getTransaction().getTransactionNumber() != null) {
            message.append(String.format("\nTransaction Number: %s\n",
                    event.getTransaction().getTransactionNumber()));
        }

        message.append("\nThank you for banking with us.\n");
        message.append("Banking System");

        return message.toString();
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
}
