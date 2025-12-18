package com.banking.account.service.interest.calculator;

import com.banking.account.module.entity.Account;
import com.banking.account.module.entity.AccountType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Penalty Interest Calculator
 *
 * Calculates penalty deductions based on account conditions such as low balance or inactivity.
 * This calculator returns negative values that reduce the total interest when used in a composite.
 */
@Component
@Slf4j
public class PenaltyInterestCalculator implements InterestCalculator {

    private static final BigDecimal LOW_BALANCE_THRESHOLD = new BigDecimal("100");
    private static final BigDecimal LOW_BALANCE_PENALTY_RATE = new BigDecimal("0.01"); // 1% penalty
    private static final BigDecimal INACTIVITY_PENALTY_RATE = new BigDecimal("0.005"); // 0.5% penalty
    private static final int INACTIVITY_DAYS_THRESHOLD = 90; // 3 months
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
    private static final int SCALE = 4;

    @Override
    public BigDecimal calculateInterest(Account account, LocalDate calculationDate) {
        if (account == null || account.getBalance() == null) {
            log.warn("Account or balance is null, returning zero penalty");
            return BigDecimal.ZERO;
        }

        BigDecimal balance = account.getBalance();
        BigDecimal penalty = BigDecimal.ZERO;

        // Low balance penalty
        if (balance.compareTo(LOW_BALANCE_THRESHOLD) < 0 && balance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal dailyPenaltyRate = LOW_BALANCE_PENALTY_RATE.divide(DAYS_IN_YEAR, SCALE, RoundingMode.HALF_UP);
            BigDecimal lowBalancePenalty = balance.multiply(dailyPenaltyRate).setScale(2, RoundingMode.HALF_UP);
            penalty = penalty.add(lowBalancePenalty);
            log.debug("Low balance penalty applied: {} for account {}", lowBalancePenalty, account.getAccountNumber());
        }

        // Inactivity penalty (if account hasn't been updated recently)
        if (account.getUpdatedAt() != null) {
            long daysSinceLastUpdate = java.time.temporal.ChronoUnit.DAYS.between(
                    account.getUpdatedAt().toLocalDate(), calculationDate);

            if (daysSinceLastUpdate >= INACTIVITY_DAYS_THRESHOLD) {
                BigDecimal dailyPenaltyRate = INACTIVITY_PENALTY_RATE.divide(DAYS_IN_YEAR, SCALE, RoundingMode.HALF_UP);
                BigDecimal inactivityPenalty = balance.multiply(dailyPenaltyRate).setScale(2, RoundingMode.HALF_UP);
                penalty = penalty.add(inactivityPenalty);
                log.debug("Inactivity penalty applied: {} for account {} (inactive for {} days)",
                        inactivityPenalty, account.getAccountNumber(), daysSinceLastUpdate);
            }
        }

        // Return negative value to reduce total interest
        BigDecimal totalPenalty = penalty.negate();
        log.debug("Total penalty calculated: {} for account {}", totalPenalty, account.getAccountNumber());
        return totalPenalty;
    }

    @Override
    public boolean supports(AccountType accountType) {
        if (accountType == null || accountType.getCode() == null) {
            return false;
        }
        // Penalty calculator supports checking and savings accounts
        return "CHECKING".equalsIgnoreCase(accountType.getCode()) ||
                "SAVINGS".equalsIgnoreCase(accountType.getCode());
    }

    @Override
    public String getCalculatorName() {
        return "PenaltyInterestCalculator";
    }

    @Override
    public BigDecimal getDefaultInterestRate() {
        // Return negative combined penalty rate
        return LOW_BALANCE_PENALTY_RATE.add(INACTIVITY_PENALTY_RATE).negate();
    }
}
