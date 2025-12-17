package com.banking.strategy.impl;

import com.banking.account.module.entity.Account;
import com.banking.account.module.entity.AccountType;
import com.banking.strategy.InterestCalculationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Slf4j
public class CheckingInterestStrategy implements InterestCalculationStrategy {

    private static final BigDecimal DEFAULT_INTEREST_RATE = BigDecimal.ZERO; // No interest by default
    private static final BigDecimal MINIMAL_INTEREST_RATE = new BigDecimal("0.001"); // 0.1% annual if enabled

    @Override
    public BigDecimal calculateInterest(Account account, LocalDate calculationDate) {
        if (account == null || account.getBalance() == null) {
            log.warn("Account or balance is null, returning zero interest");
            return BigDecimal.ZERO;
        }

        BigDecimal balance = account.getBalance();

        // Skip if balance is zero or negative
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Checking accounts typically don't earn interest
        // But if account has an interest rate set, use it
        BigDecimal annualRate = account.getInterestRate();
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Checking account {} has no interest rate, returning zero interest",
                    account.getAccountNumber());
            return BigDecimal.ZERO;
        }

        // If interest rate is set, calculate minimal daily interest
        BigDecimal dailyRate = annualRate.divide(new BigDecimal("365"), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal interest = balance.multiply(dailyRate).setScale(2, java.math.RoundingMode.HALF_UP);

        log.debug("Calculated interest for checking account {}: {} (Balance: {}, Rate: {})",
                account.getAccountNumber(), interest, balance, annualRate);

        return interest;
    }

    @Override
    public boolean supports(AccountType accountType) {
        if (accountType == null || accountType.getCode() == null) {
            return false;
        }
        return "CHECKING".equalsIgnoreCase(accountType.getCode());
    }

    @Override
    public String getStrategyName() {
        return "CheckingInterestStrategy";
    }

    @Override
    public BigDecimal getDefaultInterestRate() {
        return DEFAULT_INTEREST_RATE;
    }
}
