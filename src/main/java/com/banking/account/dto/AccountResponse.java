package com.banking.account.dto;

import com.banking.core.enums.AccountState;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountResponse {
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private AccountState state;
    private LocalDateTime createdAt;
    private Long customerId;
}
