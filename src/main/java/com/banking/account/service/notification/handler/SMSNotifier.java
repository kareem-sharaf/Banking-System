package com.banking.account.service.notification.handler;

import com.banking.account.module.entity.Account;
import com.banking.core.notification.module.entity.Notification;
import com.banking.core.auth.module.entity.User;
import com.banking.core.enums.AccountEventType;
import com.banking.core.enums.NotificationPriority;
import com.banking.core.enums.NotificationType;
import com.banking.account.service.notification.AccountObserver;
import com.banking.account.service.notification.AccountEvent;
import com.banking.core.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SMSNotifier implements AccountObserver {

    private static final Logger logger = LoggerFactory.getLogger(SMSNotifier.class);

    private final NotificationRepository notificationRepository;

    @Override
    public void update(AccountEvent event) {
        // Only send SMS for critical events
        if (!shouldSendSMS(event.getEventType())) {
            logger.debug("SMSNotifier: Skipping SMS for non-critical event: {}", event.getEventType());
            return;
        }

        try {
            Account account = event.getAccount();
            if (account == null || account.getCustomer() == null) {
                logger.warn("SMSNotifier: Account or customer is null, skipping notification");
                return;
            }

            User user = account.getCustomer().getUser();
            if (user == null || user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()) {
                logger.warn("SMSNotifier: User or phone number is null, skipping SMS notification");
                return;
            }

            // Create SMS notification message (short and concise)
            String title = "SMS Alert - " + account.getAccountNumber();
            String message = buildSMSMessage(event);

            // Create and save notification
            Notification notification = new Notification();
            notification.setUserId(user.getId());
            notification.setAccount(account);
            notification.setNotificationType(NotificationType.ACCOUNT_ALERT);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setPriority(NotificationPriority.HIGH);
            notification.setIsRead(false);
            notification.setCreatedDate(LocalDateTime.now());

            notificationRepository.save(notification);

            // Log SMS notification (in production, this would send actual SMS)
            logger.info("SMS notification sent to {} for account {} - Event: {}",
                    user.getPhoneNumber(), account.getAccountNumber(), event.getEventType());

        } catch (Exception e) {
            logger.error("Error sending SMS notification for event: {}", event.getEventType(), e);
        }
    }

    @Override
    public String getObserverType() {
        return "SMS";
    }

    /**
     * Determine if SMS should be sent for this event type
     * Only critical events trigger SMS to avoid spam
     */
    private boolean shouldSendSMS(AccountEventType eventType) {
        return switch (eventType) {
            case SUSPICIOUS_ACTIVITY, LOW_BALANCE -> true;
            case DEPOSIT_COMPLETED, WITHDRAWAL_COMPLETED, TRANSFER_COMPLETED -> false;
            default -> false;
        };
    }

    /**
     * Build concise SMS message (SMS has character limits)
     */
    private String buildSMSMessage(AccountEvent event) {
        Account account = event.getAccount();
        StringBuilder message = new StringBuilder();

        switch (event.getEventType()) {
            case SUSPICIOUS_ACTIVITY:
                message.append(String.format("ALERT: Unusual activity on acct %s. Contact us if not you.",
                        account.getAccountNumber()));
                break;

            case LOW_BALANCE:
                message.append(String.format("LOW BALANCE: Acct %s balance is %s %s. Add funds to avoid charges.",
                        account.getAccountNumber(),
                        event.getNewBalance() != null ? event.getNewBalance() : "low",
                        account.getCurrency()));
                break;

            default:
                message.append(String.format("Activity on acct %s: %s",
                        account.getAccountNumber(), event.getEventType()));
        }

        return message.toString();
    }
}
