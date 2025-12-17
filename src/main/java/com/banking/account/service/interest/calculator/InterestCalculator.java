package com.banking.account.service.interest.calculator;

import com.banking.account.module.entity.Account;
import com.banking.account.module.entity.AccountType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface InterestCalculator {

    BigDecimal calculateInterest(Account account, LocalDate calculationDate);

    boolean supports(AccountType accountType);

    String getCalculatorName();

    BigDecimal getDefaultInterestRate();
}
