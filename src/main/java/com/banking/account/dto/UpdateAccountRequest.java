package com.banking.account.dto;

import com.banking.core.enums.AccountState;
import lombok.Data;

@Data
public class UpdateAccountRequest {
    private AccountState state;
    private String accountType; // To upgrade/downgrade account
}
