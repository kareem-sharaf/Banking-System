package com.banking.account.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class AccountSummaryDto {
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private String state;
}
