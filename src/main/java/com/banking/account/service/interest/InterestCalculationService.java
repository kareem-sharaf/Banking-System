package com.banking.account.service.interest;

import com.banking.account.module.entity.Account;
import com.banking.account.module.entity.InterestCalculation;
import com.banking.core.enums.AccountEventType;
import com.banking.core.enums.AccountState;
import com.banking.core.enums.CalculationStatus;
import com.banking.core.exception.InterestCalculationException;
import com.banking.account.service.notification.AccountEvent;
import com.banking.account.service.notification.AccountSubjectManager;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.InterestCalculationRepository;
import com.banking.account.service.interest.calculator.InterestCalculator;
import com.banking.account.service.interest.calculator.BonusInterestCalculator;
import com.banking.account.service.interest.calculator.PenaltyInterestCalculator;
import com.banking.account.service.interest.calculator.CompositeInterestCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestCalculationService {

    private final AccountRepository accountRepository;
    private final InterestCalculationRepository interestCalculationRepository;
    private final List<InterestCalculator> calculators;
    private final AccountSubjectManager accountSubjectManager;
    private final BonusInterestCalculator bonusCalculator;
    private final PenaltyInterestCalculator penaltyCalculator;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void calculateDailyInterest() {
        LocalDate calculationDate = LocalDate.now();
        log.info("=== Starting daily interest calculation at {} ===", LocalDateTime.now());
        log.info("Calculation date: {}", calculationDate);

        try {
            // Get all active accounts
            List<Account> activeAccounts = accountRepository.findByState(AccountState.ACTIVE);
            log.info("Found {} active accounts for interest calculation", activeAccounts.size());

            if (activeAccounts.isEmpty()) {
                log.info("No active accounts found, skipping interest calculation");
                return;
            }

            // Counters for statistics
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failedCount = new AtomicInteger(0);
            AtomicInteger skippedCount = new AtomicInteger(0);
            AtomicReference<BigDecimal> totalInterest = new AtomicReference<>(BigDecimal.ZERO);

            // Process each account
            for (Account account : activeAccounts) {
                try {
                    CalculationResult result = calculateInterestForAccount(account, calculationDate);

                    if (result.isSkipped()) {
                        skippedCount.incrementAndGet();
                        log.debug("Skipped interest calculation for account: {}", account.getAccountNumber());
                    } else {
                        successCount.incrementAndGet();
                        totalInterest.updateAndGet(current -> current.add(result.getInterestAmount()));
                        log.debug("Successfully calculated interest for account {}: {}",
                                account.getAccountNumber(), result.getInterestAmount());
                    }
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                    log.error("Failed to calculate interest for account {}: {}",
                            account.getAccountNumber(), e.getMessage(), e);
                    saveFailedCalculation(account, calculationDate, e);
                }
            }

            // Log summary
            log.info("=== Interest calculation completed ===");
            log.info("Total accounts: {}", activeAccounts.size());
            log.info("Successful: {}", successCount.get());
            log.info("Failed: {}", failedCount.get());
            log.info("Skipped: {}", skippedCount.get());
            log.info("Total interest calculated: {}", totalInterest.get());

            // Send notification if there were failures
            if (failedCount.get() > 0) {
                log.warn("Interest calculation completed with {} failures. Review logs for details.",
                        failedCount.get());
                // In production, you might want to send an alert here
            }

        } catch (Exception e) {
            log.error("Critical error during daily interest calculation", e);
            throw new InterestCalculationException("Failed to complete daily interest calculation", e);
        }
    }

    /**
     * Calculate interest for a single account
     * 
     * @param account         The account to calculate interest for
     * @param calculationDate The date for calculation
     * @return CalculationResult containing the result
     */
    @Transactional
    public CalculationResult calculateInterestForAccount(Account account, LocalDate calculationDate) {
        if (account == null) {
            throw new InterestCalculationException("Account cannot be null");
        }

        // Check if account is active
        if (account.getState() != AccountState.ACTIVE) {
            log.debug("Skipping account {} - not active (state: {})",
                    account.getAccountNumber(), account.getState());
            return CalculationResult.skipped("Account is not active");
        }

        // Check if already calculated for this date
        if (account.getLastInterestCalculation() != null &&
                account.getLastInterestCalculation().equals(calculationDate)) {
            log.debug("Skipping account {} - already calculated for date {}",
                    account.getAccountNumber(), calculationDate);
            return CalculationResult.skipped("Already calculated for this date");
        }

        // Find suitable calculator
        InterestCalculator calculator = findSuitableCalculator(account);
        if (calculator == null) {
            log.warn("No suitable calculator found for account type: {}",
                    account.getAccountType() != null ? account.getAccountType().getCode() : "null");
            return CalculationResult.skipped("No suitable calculator found");
        }

        long startTime = System.currentTimeMillis();

        try {
            // Calculate interest
            BigDecimal previousBalance = account.getBalance();
            BigDecimal interestAmount = calculator.calculateInterest(account, calculationDate);

            // Skip if interest is zero or negative
            if (interestAmount == null || interestAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("Skipping account {} - calculated interest is zero or negative",
                        account.getAccountNumber());
                return CalculationResult.skipped("Interest amount is zero or negative");
            }

            // Update account balance
            BigDecimal newBalance = previousBalance.add(interestAmount);
            account.setBalance(newBalance);
            account.setLastInterestCalculation(calculationDate);
            account.setUpdatedAt(LocalDateTime.now());

            // Save account
            account = accountRepository.save(account);

            // Calculate duration
            long duration = System.currentTimeMillis() - startTime;

            // Save calculation record
            saveSuccessfulCalculation(account, interestAmount, calculator.getCalculatorName(),
                    calculationDate, previousBalance, newBalance, duration);

            // Notify observers about the interest calculation
            notifyInterestCalculation(account, interestAmount, previousBalance, newBalance);

            return CalculationResult.success(interestAmount);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            saveFailedCalculation(account, calculationDate, e, duration);
            throw new InterestCalculationException(
                    "Failed to calculate interest for account: " + account.getAccountNumber(), e);
        }
    }

    /**
     * Find suitable calculator for account type
     * Uses composite pattern to combine base calculator with bonus and penalty calculators
     */
    private InterestCalculator findSuitableCalculator(Account account) {
        if (account.getAccountType() == null) {
            return null;
        }

        // Find the base calculator for this account type
        InterestCalculator baseCalculator = calculators.stream()
                .filter(calculator -> calculator.supports(account.getAccountType()))
                .findFirst()
                .orElse(null);

        if (baseCalculator == null) {
            return null;
        }

        // Create a composite calculator that includes base calculator + bonuses/penalties
        CompositeInterestCalculator compositeCalculator =
                new CompositeInterestCalculator("Composite_" + baseCalculator.getCalculatorName());

        // Always add the base calculator
        compositeCalculator.addCalculator(baseCalculator);

        // Add bonus calculator for all account types
        if (bonusCalculator.supports(account.getAccountType())) {
            compositeCalculator.addCalculator(bonusCalculator);
        }

        // Add penalty calculator for applicable account types
        if (penaltyCalculator.supports(account.getAccountType())) {
            compositeCalculator.addCalculator(penaltyCalculator);
        }

        log.debug("Created composite calculator for account {} with {} calculators: base={}, bonus={}, penalty={}",
                account.getAccountNumber(),
                compositeCalculator.getCalculatorCount(),
                baseCalculator.getCalculatorName(),
                bonusCalculator.supports(account.getAccountType()),
                penaltyCalculator.supports(account.getAccountType()));

        return compositeCalculator;
    }

    /**
     * Save successful calculation record
     */
    private void saveSuccessfulCalculation(Account account, BigDecimal interestAmount,
            String calculatorName, LocalDate calculationDate,
            BigDecimal previousBalance, BigDecimal newBalance,
            long durationMs) {
        InterestCalculation calculation = new InterestCalculation();
        calculation.setAccount(account);
        calculation.setInterestAmount(interestAmount);
        calculation.setCalculationDate(calculationDate);
        calculation.setStrategyUsed(calculatorName);
        calculation.setStatus(CalculationStatus.SUCCESS);
        calculation.setPreviousBalance(previousBalance);
        calculation.setNewBalance(newBalance);
        calculation.setInterestRate(account.getInterestRate());
        calculation.setCalculationDurationMs(durationMs);
        calculation.setCreatedAt(LocalDateTime.now());

        interestCalculationRepository.save(calculation);
    }

    /**
     * Save failed calculation record
     */
    private void saveFailedCalculation(Account account, LocalDate calculationDate, Exception e) {
        saveFailedCalculation(account, calculationDate, e, 0L);
    }

    /**
     * Save failed calculation record with duration
     */
    private void saveFailedCalculation(Account account, LocalDate calculationDate,
            Exception e, long durationMs) {
        InterestCalculation calculation = new InterestCalculation();
        calculation.setAccount(account);
        calculation.setInterestAmount(BigDecimal.ZERO);
        calculation.setCalculationDate(calculationDate);
        calculation.setStrategyUsed("N/A");
        calculation.setStatus(CalculationStatus.FAILED);
        calculation.setErrorMessage(
                e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 1000))
                        : "Unknown error");
        calculation.setCalculationDurationMs(durationMs);
        calculation.setCreatedAt(LocalDateTime.now());

        interestCalculationRepository.save(calculation);
    }

    /**
     * Notify observers about interest calculation
     */
    private void notifyInterestCalculation(Account account, BigDecimal interestAmount,
            BigDecimal previousBalance, BigDecimal newBalance) {
        try {
            // Create event for deposit (interest is like a deposit)
            AccountEvent event = AccountEvent.builder()
                    .eventType(AccountEventType.DEPOSIT_COMPLETED)
                    .account(account)
                    .timestamp(LocalDateTime.now())
                    .amount(interestAmount)
                    .previousBalance(previousBalance)
                    .newBalance(newBalance)
                    .message("Daily interest calculated: " + interestAmount + " " + account.getCurrency())
                    .build();

            accountSubjectManager.notifyObservers(event);
        } catch (Exception e) {
            log.warn("Failed to notify observers about interest calculation for account {}",
                    account.getAccountNumber(), e);
        }
    }

    /**
     * Inner class to hold calculation result
     */
    public static class CalculationResult {
        private final boolean success;
        private final boolean skipped;
        private final BigDecimal interestAmount;
        private final String skipReason;

        private CalculationResult(boolean success, boolean skipped, BigDecimal interestAmount, String skipReason) {
            this.success = success;
            this.skipped = skipped;
            this.interestAmount = interestAmount;
            this.skipReason = skipReason;
        }

        public static CalculationResult success(BigDecimal interestAmount) {
            return new CalculationResult(true, false, interestAmount, null);
        }

        public static CalculationResult skipped(String reason) {
            return new CalculationResult(false, true, BigDecimal.ZERO, reason);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isSkipped() {
            return skipped;
        }

        public BigDecimal getInterestAmount() {
            return interestAmount;
        }

        public String getSkipReason() {
            return skipReason;
        }
    }
}
