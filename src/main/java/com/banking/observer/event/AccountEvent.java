package com.banking.observer.event;

import com.banking.account.module.entity.Account;
import com.banking.transaction.module.entity.Transaction;
import com.banking.core.enums.AccountEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEvent {

    private AccountEventType eventType;

    private Account account;

    private Transaction transaction;

    private LocalDateTime timestamp;

    private String message;

    private java.math.BigDecimal amount;

    private java.math.BigDecimal previousBalance;

    private java.math.BigDecimal newBalance;

    public AccountEvent(AccountEventType eventType, Account account, Transaction transaction, LocalDateTime timestamp) {
        this.eventType = eventType;
        this.account = account;
        this.transaction = transaction;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }
}
