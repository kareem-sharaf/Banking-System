package com.banking.account.service.notification;

import com.banking.account.module.entity.Account;
import com.banking.account.service.notification.handler.CompositeNotificationHandler;
import com.banking.core.enums.AccountEventType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Account Subject Manager
 * 
 * Manages observers for accounts using the Observer Pattern.
 * This service acts as a centralized manager for attaching/detaching observers
 * and notifying them about account events.
 * 
 * Uses thread-safe collections to support concurrent access.
 * 
 * @author Banking System
 */
@Service
@RequiredArgsConstructor
public class AccountSubjectManager implements AccountSubject {

    private static final Logger logger = LoggerFactory.getLogger(AccountSubjectManager.class);

    // Map to store observers for each account
    // Key: Account ID, Value: List of observers
    private final ConcurrentHashMap<Long, List<AccountObserver>> accountObservers = new ConcurrentHashMap<>();

    // All available observers (injected by Spring)
    private final List<AccountObserver> allObservers;

    /**
     * Attach all available observers to an account
     * 
     * @param account The account to attach observers to
     */
    public void attachAllObservers(Account account) {
        if (account == null || account.getId() == null) {
            logger.warn("Cannot attach observers: Account or account ID is null");
            return;
        }

        Long accountId = account.getId();

        // Get or create observer list for this account
        List<AccountObserver> observers = accountObservers.computeIfAbsent(
                accountId,
                k -> new CopyOnWriteArrayList<>());

        // Add all observers if not already present
        for (AccountObserver observer : allObservers) {
            if (!observers.contains(observer)) {
                observers.add(observer);
                logger.debug("Attached observer {} to account {}",
                        observer.getObserverType(), account.getAccountNumber());
            }
        }
    }

    /**
     * Attach a specific observer to an account
     */
    @Override
    public void attach(AccountObserver observer) {
        if (observer == null) {
            logger.warn("Cannot attach null observer");
            return;
        }

        // Attach to all accounts (or you can make it account-specific)
        // For simplicity, we'll attach to all accounts when attach is called
        // In a more sophisticated implementation, you might want account-specific
        // attachment
        logger.debug("Observer {} attached globally", observer.getObserverType());
    }

    /**
     * Attach observer to a specific account
     */
    public void attach(Account account, AccountObserver observer) {
        if (account == null || account.getId() == null || observer == null) {
            logger.warn("Cannot attach observer: Account, account ID, or observer is null");
            return;
        }

        Long accountId = account.getId();
        List<AccountObserver> observers = accountObservers.computeIfAbsent(
                accountId,
                k -> new CopyOnWriteArrayList<>());

        if (!observers.contains(observer)) {
            observers.add(observer);
            logger.debug("Attached observer {} to account {}",
                    observer.getObserverType(), account.getAccountNumber());
        }
    }

    /**
     * Detach observer from an account
     */
    @Override
    public void detach(AccountObserver observer) {
        if (observer == null) {
            return;
        }

        // Remove from all accounts
        accountObservers.values().forEach(observers -> observers.remove(observer));
        logger.debug("Observer {} detached from all accounts", observer.getObserverType());
    }

    /**
     * Detach observer from a specific account
     */
    public void detach(Account account, AccountObserver observer) {
        if (account == null || account.getId() == null || observer == null) {
            return;
        }

        Long accountId = account.getId();
        List<AccountObserver> observers = accountObservers.get(accountId);

        if (observers != null) {
            observers.remove(observer);
            logger.debug("Detached observer {} from account {}",
                    observer.getObserverType(), account.getAccountNumber());
        }
    }

    /**
     * Notify all observers about an account event
     */
    @Override
    public void notifyObservers(AccountEvent event) {
        if (event == null || event.getAccount() == null) {
            logger.warn("Cannot notify observers: Event or account is null");
            return;
        }

        Account account = event.getAccount();
        Long accountId = account.getId();

        if (accountId == null) {
            logger.warn("Cannot notify observers: Account ID is null");
            return;
        }

        // Get observers for this account
        List<AccountObserver> observers = accountObservers.get(accountId);

        // If no specific observers, use all observers
        if (observers == null || observers.isEmpty()) {
            observers = allObservers;
        }

        // Notify all observers
        logger.debug("Notifying {} observers about event {} for account {}",
                observers.size(), event.getEventType(), account.getAccountNumber());

        for (AccountObserver observer : observers) {
            try {
                observer.update(event);
            } catch (Exception e) {
                // Log error but continue notifying other observers
                logger.error("Error notifying observer {} for account {}: {}",
                        observer.getObserverType(), account.getAccountNumber(), e.getMessage(), e);
            }
        }
    }

    /**
     * Clear observers for an account (useful for cleanup)
     */
    public void clearObservers(Account account) {
        if (account == null || account.getId() == null) {
            return;
        }

        accountObservers.remove(account.getId());
        logger.debug("Cleared observers for account {}", account.getAccountNumber());
    }

    /**
     * Attach composite notification handler for urgent events
     * Creates a composite that includes email, SMS, and in-app notifications
     */
    public void attachUrgentNotificationHandler(Account account) {
        if (account == null || account.getId() == null) {
            logger.warn("Cannot attach urgent notification handler: Account or account ID is null");
            return;
        }

        // Find available handlers
        AccountObserver emailHandler = allObservers.stream()
                .filter(observer -> "EMAIL".equals(observer.getObserverType()))
                .findFirst().orElse(null);

        AccountObserver smsHandler = allObservers.stream()
                .filter(observer -> "SMS".equals(observer.getObserverType()))
                .findFirst().orElse(null);

        AccountObserver inAppHandler = allObservers.stream()
                .filter(observer -> "IN_APP".equals(observer.getObserverType()))
                .findFirst().orElse(null);

        if (emailHandler != null || smsHandler != null || inAppHandler != null) {
            CompositeNotificationHandler urgentHandler =
                    CompositeNotificationHandler.createUrgentNotificationHandler(
                            emailHandler, smsHandler, inAppHandler);

            attach(account, urgentHandler);
            logger.debug("Attached urgent notification composite to account {}", account.getAccountNumber());
        } else {
            logger.warn("No notification handlers available for urgent notifications");
        }
    }

    /**
     * Attach composite notification handler for standard events
     * Creates a composite that includes email and in-app notifications
     */
    public void attachStandardNotificationHandler(Account account) {
        if (account == null || account.getId() == null) {
            logger.warn("Cannot attach standard notification handler: Account or account ID is null");
            return;
        }

        // Find available handlers
        AccountObserver emailHandler = allObservers.stream()
                .filter(observer -> "EMAIL".equals(observer.getObserverType()))
                .findFirst().orElse(null);

        AccountObserver inAppHandler = allObservers.stream()
                .filter(observer -> "IN_APP".equals(observer.getObserverType()))
                .findFirst().orElse(null);

        if (emailHandler != null || inAppHandler != null) {
            CompositeNotificationHandler standardHandler =
                    CompositeNotificationHandler.createStandardNotificationHandler(
                            emailHandler, inAppHandler);

            attach(account, standardHandler);
            logger.debug("Attached standard notification composite to account {}", account.getAccountNumber());
        } else {
            logger.warn("No notification handlers available for standard notifications");
        }
    }

    /**
     * Attach appropriate notification handler based on event type
     */
    public void attachNotificationHandlerForEvent(Account account, AccountEventType eventType) {
        if (eventType == null) {
            attachStandardNotificationHandler(account);
            return;
        }

        switch (eventType) {
            case SUSPICIOUS_ACTIVITY:
            case LOW_BALANCE:
                attachUrgentNotificationHandler(account);
                break;
            default:
                attachStandardNotificationHandler(account);
                break;
        }
    }

    /**
     * Create a custom composite notification handler
     */
    public CompositeNotificationHandler createCustomCompositeHandler(String name,
            AccountObserver... handlers) {
        CompositeNotificationHandler composite = new CompositeNotificationHandler(name);
        for (AccountObserver handler : handlers) {
            if (handler != null) {
                composite.addHandler(handler);
            }
        }
        logger.debug("Created custom composite notification handler {} with {} handlers",
                name, composite.getHandlerCount());
        return composite;
    }
}
