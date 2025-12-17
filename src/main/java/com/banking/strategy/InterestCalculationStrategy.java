package com.banking.strategy;

import com.banking.account.module.entity.Account;
import com.banking.account.module.entity.AccountType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface InterestCalculationStrategy {

    BigDecimal calculateInterest(Account account, LocalDate calculationDate);

    boolean supports(AccountType accountType);

    String getStrategyName();

    BigDecimal getDefaultInterestRate();
}
