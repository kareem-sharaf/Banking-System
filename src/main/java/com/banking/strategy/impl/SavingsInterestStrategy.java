package com.banking.strategy.impl;

import com.banking.account.module.entity.Account;
import com.banking.account.module.entity.AccountType;
import com.banking.strategy.InterestCalculationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class SavingsInterestStrategy implements InterestCalculationStrategy {

    private static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("0.03"); // 3% annual
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
    private static final int SCALE = 4;

    @Override
    public BigDecimal calculateInterest(Account account, LocalDate calculationDate) {
        if (account == null || account.getBalance() == null) {
            log.warn("Account or balance is null, returning zero interest");
            return BigDecimal.ZERO;
        }

        BigDecimal balance = account.getBalance();

        // Skip if balance is zero or negative
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Account {} has zero or negative balance, skipping interest calculation",
                    account.getAccountNumber());
            return BigDecimal.ZERO;
        }

        // Get interest rate (use account rate or default)
        BigDecimal annualRate = account.getInterestRate();
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) <= 0) {
            annualRate = DEFAULT_INTEREST_RATE;
            log.debug("Using default interest rate {} for account {}",
                    DEFAULT_INTEREST_RATE, account.getAccountNumber());
        }

        // Calculate daily interest: Balance * (Annual Rate / 365)
        BigDecimal dailyRate = annualRate.divide(DAYS_IN_YEAR, SCALE, RoundingMode.HALF_UP);
        BigDecimal interest = balance.multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);

        log.debug("Calculated interest for savings account {}: {} (Balance: {}, Rate: {})",
                account.getAccountNumber(), interest, balance, annualRate);

        return interest;
    }

    @Override
    public boolean supports(AccountType accountType) {
        if (accountType == null || accountType.getCode() == null) {
            return false;
        }
        return "SAVINGS".equalsIgnoreCase(accountType.getCode()) ||
                "CHILD_SAVINGS".equalsIgnoreCase(accountType.getCode());
    }

    @Override
    public String getStrategyName() {
        return "SavingsInterestStrategy";
    }

    @Override
    public BigDecimal getDefaultInterestRate() {
        return DEFAULT_INTEREST_RATE;
    }
}
