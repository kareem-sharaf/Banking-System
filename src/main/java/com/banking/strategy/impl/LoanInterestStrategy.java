package com.banking.strategy.impl;

import com.banking.entity.Account;
import com.banking.entity.AccountType;
import com.banking.strategy.InterestCalculationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Component
@Slf4j
public class LoanInterestStrategy implements InterestCalculationStrategy {

    private static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("0.08"); // 8% annual
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
    private static final int SCALE = 4;

    @Override
    public BigDecimal calculateInterest(Account account, LocalDate calculationDate) {
        if (account == null || account.getBalance() == null) {
            log.warn("Account or balance is null, returning zero interest");
            return BigDecimal.ZERO;
        }

        BigDecimal balance = account.getBalance();

        // For loans, balance is typically negative (amount owed)
        // If balance is zero or positive, no interest is charged
        if (balance.compareTo(BigDecimal.ZERO) >= 0) {
            log.debug("Loan account {} has zero or positive balance, no interest to charge",
                    account.getAccountNumber());
            return BigDecimal.ZERO;
        }

        // Get interest rate (use account rate or default)
        BigDecimal annualRate = account.getInterestRate();
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) <= 0) {
            annualRate = DEFAULT_INTEREST_RATE;
            log.debug("Using default loan interest rate {} for account {}",
                    DEFAULT_INTEREST_RATE, account.getAccountNumber());
        }

        // Calculate interest on absolute value of balance (outstanding loan amount)
        BigDecimal outstandingAmount = balance.abs();
        BigDecimal dailyRate = annualRate.divide(DAYS_IN_YEAR, SCALE, RoundingMode.HALF_UP);
        BigDecimal interest = outstandingAmount.multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);

        // For loans, interest increases the debt (makes balance more negative)
        // So we return positive interest amount that will be added to the negative
        // balance
        log.debug("Calculated interest for loan account {}: {} (Outstanding: {}, Rate: {})",
                account.getAccountNumber(), interest, outstandingAmount, annualRate);

        return interest;
    }

    @Override
    public boolean supports(AccountType accountType) {
        if (accountType == null || accountType.getCode() == null) {
            return false;
        }
        return "LOAN".equalsIgnoreCase(accountType.getCode());
    }

    @Override
    public String getStrategyName() {
        return "LoanInterestStrategy";
    }

    @Override
    public BigDecimal getDefaultInterestRate() {
        return DEFAULT_INTEREST_RATE;
    }
}
