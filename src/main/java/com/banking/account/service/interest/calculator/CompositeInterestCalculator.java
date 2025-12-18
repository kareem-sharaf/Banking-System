package com.banking.account.service.interest.calculator;

import com.banking.account.module.entity.Account;
import com.banking.account.module.entity.AccountType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Composite Interest Calculator
 *
 * Implements the Composite pattern to combine multiple interest calculation strategies.
 * This calculator can contain other calculators and sum their results.
 *
 * Useful for scenarios like:
 * - Base interest + bonus interest + penalty adjustments
 * - Account-level + customer-level + global-level calculations
 */
@Component
@Slf4j
public class CompositeInterestCalculator implements InterestCalculator {

    private final List<InterestCalculator> calculators = new ArrayList<>();
    private final String compositeName;

    public CompositeInterestCalculator(String compositeName) {
        this.compositeName = compositeName;
    }

    public CompositeInterestCalculator() {
        this.compositeName = "CompositeInterestCalculator";
    }

    /**
     * Add a calculator to this composite
     */
    public void addCalculator(InterestCalculator calculator) {
        if (calculator != null) {
            calculators.add(calculator);
            log.debug("Added calculator {} to composite {}", calculator.getCalculatorName(), compositeName);
        }
    }

    /**
     * Remove a calculator from this composite
     */
    public void removeCalculator(InterestCalculator calculator) {
        calculators.remove(calculator);
        log.debug("Removed calculator {} from composite {}", calculator.getCalculatorName(), compositeName);
    }

    /**
     * Get all calculators in this composite
     */
    public List<InterestCalculator> getCalculators() {
        return new ArrayList<>(calculators);
    }

    /**
     * Clear all calculators
     */
    public void clearCalculators() {
        calculators.clear();
        log.debug("Cleared all calculators from composite {}", compositeName);
    }

    @Override
    public BigDecimal calculateInterest(Account account, LocalDate calculationDate) {
        if (calculators.isEmpty()) {
            log.debug("No calculators in composite {}, returning zero interest", compositeName);
            return BigDecimal.ZERO;
        }

        BigDecimal totalInterest = BigDecimal.ZERO;
        log.debug("Calculating composite interest for account {} using {} calculators",
                account.getAccountNumber(), calculators.size());

        for (InterestCalculator calculator : calculators) {
            try {
                BigDecimal interest = calculator.calculateInterest(account, calculationDate);
                if (interest != null) {
                    totalInterest = totalInterest.add(interest);
                    log.debug("Calculator {} contributed {} interest",
                            calculator.getCalculatorName(), interest);
                }
            } catch (Exception e) {
                log.error("Error calculating interest with calculator {}: {}",
                        calculator.getCalculatorName(), e.getMessage(), e);
                // Continue with other calculators instead of failing completely
            }
        }

        log.debug("Total composite interest calculated: {} for account {}",
                totalInterest, account.getAccountNumber());

        return totalInterest;
    }

    @Override
    public boolean supports(AccountType accountType) {
        if (accountType == null) {
            return false;
        }

        // Composite supports an account type if at least one of its calculators supports it
        return calculators.stream()
                .anyMatch(calculator -> calculator.supports(accountType));
    }

    @Override
    public String getCalculatorName() {
        return compositeName;
    }

    @Override
    public BigDecimal getDefaultInterestRate() {
        if (calculators.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Return the average of all default rates
        BigDecimal totalRate = calculators.stream()
                .map(InterestCalculator::getDefaultInterestRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalRate.divide(BigDecimal.valueOf(calculators.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * Get the number of calculators in this composite
     */
    public int getCalculatorCount() {
        return calculators.size();
    }

    /**
     * Check if composite is empty
     */
    public boolean isEmpty() {
        return calculators.isEmpty();
    }
}
