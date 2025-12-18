package com.banking.account.service.interest.calculator;

import com.banking.account.module.entity.Account;
import com.banking.account.module.entity.AccountType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Bonus Interest Calculator
 *
 * Calculates bonus interest based on account balance thresholds and customer loyalty.
 * This calculator can be combined with other calculators in a composite pattern.
 */
@Component
@Slf4j
public class BonusInterestCalculator implements InterestCalculator {

    private static final BigDecimal HIGH_BALANCE_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal HIGH_BALANCE_BONUS_RATE = new BigDecimal("0.005"); // 0.5% bonus
    private static final BigDecimal LOYALTY_BONUS_RATE = new BigDecimal("0.002"); // 0.2% bonus
    private static final int LOYALTY_MONTHS_THRESHOLD = 12; // 1 year
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
    private static final int SCALE = 4;

    @Override
    public BigDecimal calculateInterest(Account account, LocalDate calculationDate) {
        if (account == null || account.getBalance() == null) {
            log.warn("Account or balance is null, returning zero bonus interest");
            return BigDecimal.ZERO;
        }

        BigDecimal balance = account.getBalance();
        BigDecimal bonusInterest = BigDecimal.ZERO;

        // High balance bonus
        if (balance.compareTo(HIGH_BALANCE_THRESHOLD) >= 0) {
            BigDecimal dailyBonusRate = HIGH_BALANCE_BONUS_RATE.divide(DAYS_IN_YEAR, SCALE, RoundingMode.HALF_UP);
            BigDecimal highBalanceBonus = balance.multiply(dailyBonusRate).setScale(2, RoundingMode.HALF_UP);
            bonusInterest = bonusInterest.add(highBalanceBonus);
            log.debug("High balance bonus applied: {} for account {}", highBalanceBonus, account.getAccountNumber());
        }

        // Loyalty bonus (if account is older than threshold)
        if (account.getCreatedAt() != null) {
            long accountAgeMonths = java.time.temporal.ChronoUnit.MONTHS.between(
                    account.getCreatedAt().toLocalDate(), calculationDate);

            if (accountAgeMonths >= LOYALTY_MONTHS_THRESHOLD) {
                BigDecimal dailyLoyaltyRate = LOYALTY_BONUS_RATE.divide(DAYS_IN_YEAR, SCALE, RoundingMode.HALF_UP);
                BigDecimal loyaltyBonus = balance.multiply(dailyLoyaltyRate).setScale(2, RoundingMode.HALF_UP);
                bonusInterest = bonusInterest.add(loyaltyBonus);
                log.debug("Loyalty bonus applied: {} for account {} (age: {} months)",
                        loyaltyBonus, account.getAccountNumber(), accountAgeMonths);
            }
        }

        log.debug("Total bonus interest calculated: {} for account {}", bonusInterest, account.getAccountNumber());
        return bonusInterest;
    }

    @Override
    public boolean supports(AccountType accountType) {
        if (accountType == null || accountType.getCode() == null) {
            return false;
        }
        // Bonus calculator supports all account types
        return true;
    }

    @Override
    public String getCalculatorName() {
        return "BonusInterestCalculator";
    }

    @Override
    public BigDecimal getDefaultInterestRate() {
        // Return combined bonus rate
        return HIGH_BALANCE_BONUS_RATE.add(LOYALTY_BONUS_RATE);
    }
}
