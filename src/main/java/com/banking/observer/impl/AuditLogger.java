package com.banking.observer.impl;

import com.banking.account.module.entity.Account;
import com.banking.core.module.entity.AuditLog;
import com.banking.transaction.module.entity.Transaction;
import com.banking.core.auth.module.entity.User;
import com.banking.core.enums.AccountEventType;
import com.banking.core.enums.ActionType;
import com.banking.observer.AccountObserver;
import com.banking.observer.event.AccountEvent;
import com.banking.core.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuditLogger implements AccountObserver {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);

    private final AuditLogRepository auditLogRepository;

    @Override
    public void update(AccountEvent event) {
        try {
            Account account = event.getAccount();
            if (account == null) {
                logger.error("AuditLogger: Account is null, cannot create audit log");
                return;
            }

            // Get user ID from account's customer
            Long userId = null;
            if (account.getCustomer() != null && account.getCustomer().getUser() != null) {
                userId = account.getCustomer().getUser().getId();
            }

            // If no user found, use a system user ID (0) or get from transaction
            if (userId == null && event.getTransaction() != null) {
                // Try to get user from transaction context if available
                userId = 0L; // System user
            }

            // Map event type to action type
            ActionType actionType = mapEventTypeToActionType(event.getEventType());

            // Build action description
            String action = buildActionDescription(event);

            // Build audit details
            String details = buildAuditDetails(event);

            // Create audit log entry
            AuditLog auditLog = new AuditLog();
            auditLog.setAccount(account);
            auditLog.setTransaction(event.getTransaction());
            auditLog.setUserId(userId != null ? userId : 0L);
            auditLog.setAction(action);
            auditLog.setActionType(actionType);
            auditLog.setDetails(details);
            auditLog.setTimestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now());
            auditLog.setStatus("SUCCESS");

            auditLogRepository.save(auditLog);

            logger.debug("Audit log created for account {} - Event: {}",
                    account.getAccountNumber(), event.getEventType());

        } catch (Exception e) {
            // Audit logging should never fail silently
            logger.error("CRITICAL: Failed to create audit log for event: {}", event.getEventType(), e);
            // In production, you might want to send an alert to administrators
        }
    }

    @Override
    public String getObserverType() {
        return "AUDIT";
    }

    /**
     * Map AccountEventType to ActionType for audit logs
     */
    private ActionType mapEventTypeToActionType(AccountEventType eventType) {
        return switch (eventType) {
            case DEPOSIT_COMPLETED -> ActionType.DEPOSIT;
            case WITHDRAWAL_COMPLETED -> ActionType.WITHDRAWAL;
            case TRANSFER_COMPLETED -> ActionType.TRANSFER;
            case LOW_BALANCE -> ActionType.VIEW; // Balance check
            case SUSPICIOUS_ACTIVITY -> ActionType.VIEW; // Security check
            case ACCOUNT_OPENED -> ActionType.CREATE;
            case ACCOUNT_CLOSED -> ActionType.DELETE;
            case TRANSACTION_FAILED -> ActionType.REJECT;
            default -> ActionType.VIEW;
        };
    }

    /**
     * Build action description for audit log
     */
    private String buildActionDescription(AccountEvent event) {
        Account account = event.getAccount();
        StringBuilder action = new StringBuilder();

        switch (event.getEventType()) {
            case DEPOSIT_COMPLETED:
                action.append(String.format("Deposit completed on account %s", account.getAccountNumber()));
                break;
            case WITHDRAWAL_COMPLETED:
                action.append(String.format("Withdrawal completed on account %s", account.getAccountNumber()));
                break;
            case TRANSFER_COMPLETED:
                action.append(String.format("Transfer completed involving account %s", account.getAccountNumber()));
                break;
            case LOW_BALANCE:
                action.append(String.format("Low balance alert for account %s", account.getAccountNumber()));
                break;
            case SUSPICIOUS_ACTIVITY:
                action.append(String.format("Suspicious activity detected on account %s", account.getAccountNumber()));
                break;
            default:
                action.append(String.format("Account event: %s on account %s",
                        event.getEventType(), account.getAccountNumber()));
        }

        return action.toString();
    }

    /**
     * Build detailed audit information
     */
    private String buildAuditDetails(AccountEvent event) {
        Account account = event.getAccount();
        StringBuilder details = new StringBuilder();

        details.append(String.format("Event Type: %s\n", event.getEventType()));
        details.append(String.format("Account Number: %s\n", account.getAccountNumber()));
        details.append(String.format("Account Type: %s\n",
                account.getAccountType() != null ? account.getAccountType().getName() : "N/A"));
        details.append(String.format("Currency: %s\n", account.getCurrency()));

        if (event.getAmount() != null) {
            details.append(String.format("Amount: %s %s\n", event.getAmount(), account.getCurrency()));
        }

        if (event.getPreviousBalance() != null) {
            details.append(String.format("Previous Balance: %s %s\n",
                    event.getPreviousBalance(), account.getCurrency()));
        }

        if (event.getNewBalance() != null) {
            details.append(String.format("New Balance: %s %s\n",
                    event.getNewBalance(), account.getCurrency()));
        }

        if (event.getTransaction() != null) {
            Transaction transaction = event.getTransaction();
            details.append(String.format("Transaction Number: %s\n", transaction.getTransactionNumber()));
            details.append(String.format("Transaction Type: %s\n", transaction.getTransactionType()));
            details.append(String.format("Transaction Status: %s\n", transaction.getStatus()));

            if (transaction.getDescription() != null) {
                details.append(String.format("Transaction Description: %s\n", transaction.getDescription()));
            }
        }

        if (event.getMessage() != null) {
            details.append(String.format("Event Message: %s\n", event.getMessage()));
        }

        details.append(String.format("Timestamp: %s", event.getTimestamp()));

        return details.toString();
    }
}
