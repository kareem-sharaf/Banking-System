package com.banking.account.service.notification.handler;

import com.banking.account.service.notification.AccountObserver;
import com.banking.account.service.notification.AccountEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite Notification Handler
 *
 * Implements the Composite pattern to combine multiple notification handlers.
 * This handler can contain other handlers and delegate notifications to all of them.
 *
 * Useful for scenarios like:
 * - Multi-channel notifications (email + SMS + in-app)
 * - Conditional notification routing (urgent notifications to multiple channels)
 * - Notification groups and hierarchies
 */
@Component
@Slf4j
public class CompositeNotificationHandler implements AccountObserver {

    private final List<AccountObserver> handlers = new ArrayList<>();
    private final String compositeName;

    public CompositeNotificationHandler(String compositeName) {
        this.compositeName = compositeName;
    }

    public CompositeNotificationHandler() {
        this.compositeName = "CompositeNotificationHandler";
    }

    /**
     * Add a notification handler to this composite
     */
    public void addHandler(AccountObserver handler) {
        if (handler != null) {
            handlers.add(handler);
            log.debug("Added notification handler {} to composite {}", handler.getObserverType(), compositeName);
        }
    }

    /**
     * Remove a notification handler from this composite
     */
    public void removeHandler(AccountObserver handler) {
        handlers.remove(handler);
        log.debug("Removed notification handler {} from composite {}", handler.getObserverType(), compositeName);
    }

    /**
     * Get all handlers in this composite
     */
    public List<AccountObserver> getHandlers() {
        return new ArrayList<>(handlers);
    }

    /**
     * Clear all handlers
     */
    public void clearHandlers() {
        handlers.clear();
        log.debug("Cleared all handlers from composite {}", compositeName);
    }

    @Override
    public void update(AccountEvent event) {
        if (handlers.isEmpty()) {
            log.debug("No handlers in composite {}, skipping notification", compositeName);
            return;
        }

        log.debug("Sending notification through composite {} to {} handlers",
                compositeName, handlers.size());

        for (AccountObserver handler : handlers) {
            try {
                handler.update(event);
                log.debug("Notification sent via handler: {}", handler.getObserverType());
            } catch (Exception e) {
                log.error("Error sending notification via handler {}: {}",
                        handler.getObserverType(), e.getMessage(), e);
                // Continue with other handlers instead of failing completely
            }
        }
    }

    @Override
    public String getObserverType() {
        return "COMPOSITE_" + compositeName.toUpperCase();
    }

    /**
     * Get the number of handlers in this composite
     */
    public int getHandlerCount() {
        return handlers.size();
    }

    /**
     * Check if composite is empty
     */
    public boolean isEmpty() {
        return handlers.isEmpty();
    }

    /**
     * Create a multi-channel notification composite for urgent events
     */
    public static CompositeNotificationHandler createUrgentNotificationHandler(
            AccountObserver emailHandler, AccountObserver smsHandler, AccountObserver inAppHandler) {

        CompositeNotificationHandler urgentHandler = new CompositeNotificationHandler("UrgentNotifications");
        urgentHandler.addHandler(emailHandler);
        urgentHandler.addHandler(smsHandler);
        urgentHandler.addHandler(inAppHandler);
        return urgentHandler;
    }

    /**
     * Create a standard notification composite
     */
    public static CompositeNotificationHandler createStandardNotificationHandler(
            AccountObserver emailHandler, AccountObserver inAppHandler) {

        CompositeNotificationHandler standardHandler = new CompositeNotificationHandler("StandardNotifications");
        standardHandler.addHandler(emailHandler);
        standardHandler.addHandler(inAppHandler);
        return standardHandler;
    }
}
