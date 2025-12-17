package com.banking.account.controller;

import com.banking.account.dto.InterestCalculationResponse;
import com.banking.account.dto.InterestCalculationSummary;
import com.banking.account.module.entity.Account;
import com.banking.account.module.entity.InterestCalculation;
import com.banking.core.enums.CalculationStatus;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.InterestCalculationRepository;
import com.banking.account.service.DailyInterestCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interest Calculation Controller
 * 
 * REST API endpoints for manual interest calculation and reporting.
 * 
 * @author Banking System
 */
@RestController
@RequestMapping("/api/interest")
@RequiredArgsConstructor
@Slf4j
public class InterestCalculationController {

        private final DailyInterestCalculationService interestCalculationService;
        private final InterestCalculationRepository interestCalculationRepository;
        private final AccountRepository accountRepository;

        /**
         * Trigger manual interest calculation
         * 
         * POST /api/interest/calculate-now
         * 
         * Requires: ADMIN role
         */
        @PostMapping("/calculate-now")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<InterestCalculationResponse> triggerManualCalculation() {
                log.info("Manual interest calculation triggered by admin");

                try {
                        LocalDate calculationDate = LocalDate.now();
                        LocalDateTime startTime = LocalDateTime.now();

                        // Execute calculation
                        interestCalculationService.calculateDailyInterest();

                        LocalDateTime endTime = LocalDateTime.now();

                        // Get statistics
                        List<InterestCalculation> todayCalculations = interestCalculationRepository
                                        .findByCalculationDate(calculationDate);

                        long successCount = todayCalculations.stream()
                                        .filter(ic -> ic.getStatus() == CalculationStatus.SUCCESS)
                                        .count();

                        long failedCount = todayCalculations.stream()
                                        .filter(ic -> ic.getStatus() == CalculationStatus.FAILED)
                                        .count();

                        BigDecimal totalInterest = todayCalculations.stream()
                                        .filter(ic -> ic.getStatus() == CalculationStatus.SUCCESS)
                                        .map(InterestCalculation::getInterestAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        InterestCalculationResponse response = InterestCalculationResponse.builder()
                                        .success(true)
                                        .message("Interest calculation completed successfully")
                                        .calculationTime(endTime)
                                        .totalAccounts((int) todayCalculations.size())
                                        .successfulCalculations((int) successCount)
                                        .failedCalculations((int) failedCount)
                                        .skippedAccounts(0)
                                        .totalInterestCalculated(totalInterest)
                                        .calculationDate(calculationDate)
                                        .build();

                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        log.error("Manual interest calculation failed", e);

                        InterestCalculationResponse response = InterestCalculationResponse.builder()
                                        .success(false)
                                        .message("Interest calculation failed: " + e.getMessage())
                                        .calculationTime(LocalDateTime.now())
                                        .totalAccounts(0)
                                        .successfulCalculations(0)
                                        .failedCalculations(0)
                                        .skippedAccounts(0)
                                        .totalInterestCalculated(BigDecimal.ZERO)
                                        .calculationDate(LocalDate.now())
                                        .build();

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
        }

        /**
         * Calculate interest for a specific account
         * 
         * POST /api/interest/calculate/{accountNumber}
         * 
         * Requires: ADMIN or MANAGER role
         */
        @PostMapping("/calculate/{accountNumber}")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
        public ResponseEntity<InterestCalculationResponse> calculateForAccount(
                        @PathVariable String accountNumber) {

                log.info("Manual interest calculation triggered for account: {}", accountNumber);

                try {
                        Account account = accountRepository.findByAccountNumber(accountNumber)
                                        .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));

                        DailyInterestCalculationService.CalculationResult result = interestCalculationService
                                        .calculateInterestForAccount(account, LocalDate.now());

                        if (result.isSkipped()) {
                                InterestCalculationResponse response = InterestCalculationResponse.builder()
                                                .success(false)
                                                .message("Calculation skipped: " + result.getSkipReason())
                                                .calculationTime(LocalDateTime.now())
                                                .totalAccounts(1)
                                                .successfulCalculations(0)
                                                .failedCalculations(0)
                                                .skippedAccounts(1)
                                                .totalInterestCalculated(BigDecimal.ZERO)
                                                .calculationDate(LocalDate.now())
                                                .build();

                                return ResponseEntity.ok(response);
                        }

                        InterestCalculationResponse response = InterestCalculationResponse.builder()
                                        .success(true)
                                        .message("Interest calculated successfully")
                                        .calculationTime(LocalDateTime.now())
                                        .totalAccounts(1)
                                        .successfulCalculations(1)
                                        .failedCalculations(0)
                                        .skippedAccounts(0)
                                        .totalInterestCalculated(result.getInterestAmount())
                                        .calculationDate(LocalDate.now())
                                        .build();

                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        log.error("Failed to calculate interest for account: {}", accountNumber, e);

                        InterestCalculationResponse response = InterestCalculationResponse.builder()
                                        .success(false)
                                        .message("Calculation failed: " + e.getMessage())
                                        .calculationTime(LocalDateTime.now())
                                        .totalAccounts(1)
                                        .successfulCalculations(0)
                                        .failedCalculations(1)
                                        .skippedAccounts(0)
                                        .totalInterestCalculated(BigDecimal.ZERO)
                                        .calculationDate(LocalDate.now())
                                        .build();

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
        }

        /**
         * Get last calculation summary
         * 
         * GET /api/interest/last-calculation
         * 
         * Requires: Any authenticated user
         */
        @GetMapping("/last-calculation")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<InterestCalculationSummary> getLastCalculationSummary() {
                log.info("Retrieving last interest calculation summary");

                LocalDate today = LocalDate.now();
                List<InterestCalculation> calculations = interestCalculationRepository.findByCalculationDate(today);

                if (calculations.isEmpty()) {
                        // Try yesterday
                        LocalDate yesterday = today.minusDays(1);
                        calculations = interestCalculationRepository.findByCalculationDate(yesterday);
                }

                if (calculations.isEmpty()) {
                        return ResponseEntity.ok(InterestCalculationSummary.builder()
                                        .calculationDate(today)
                                        .totalAccounts(0)
                                        .successfulCalculations(0)
                                        .failedCalculations(0)
                                        .skippedAccounts(0)
                                        .totalInterestCalculated(BigDecimal.ZERO)
                                        .build());
                }

                LocalDate calculationDate = calculations.get(0).getCalculationDate();

                long successCount = calculations.stream()
                                .filter(ic -> ic.getStatus() == CalculationStatus.SUCCESS)
                                .count();

                long failedCount = calculations.stream()
                                .filter(ic -> ic.getStatus() == CalculationStatus.FAILED)
                                .count();

                BigDecimal totalInterest = calculations.stream()
                                .filter(ic -> ic.getStatus() == CalculationStatus.SUCCESS)
                                .map(InterestCalculation::getInterestAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                List<InterestCalculationSummary.AccountCalculationDetail> details = calculations.stream()
                                .map(ic -> InterestCalculationSummary.AccountCalculationDetail.builder()
                                                .accountNumber(ic.getAccount().getAccountNumber())
                                                .accountType(ic.getAccount().getAccountType() != null
                                                                ? ic.getAccount().getAccountType().getCode()
                                                                : "N/A")
                                                .interestAmount(ic.getInterestAmount())
                                                .previousBalance(ic.getPreviousBalance())
                                                .newBalance(ic.getNewBalance())
                                                .status(ic.getStatus().toString())
                                                .errorMessage(ic.getErrorMessage())
                                                .build())
                                .collect(Collectors.toList());

                InterestCalculationSummary summary = InterestCalculationSummary.builder()
                                .calculationDate(calculationDate)
                                .totalAccounts(calculations.size())
                                .successfulCalculations((int) successCount)
                                .failedCalculations((int) failedCount)
                                .skippedAccounts(0)
                                .totalInterestCalculated(totalInterest)
                                .accountDetails(details)
                                .build();

                return ResponseEntity.ok(summary);
        }

        /**
         * Get calculation history for a date range
         * 
         * GET /api/interest/history?startDate=2024-01-01&endDate=2024-01-31
         * 
         * Requires: ADMIN or MANAGER role
         */
        @GetMapping("/history")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
        public ResponseEntity<List<InterestCalculation>> getCalculationHistory(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

                log.info("Retrieving interest calculation history from {} to {}", startDate, endDate);

                List<InterestCalculation> calculations = interestCalculationRepository
                                .findByCalculationDateBetween(startDate, endDate);

                return ResponseEntity.ok(calculations);
        }

        /**
         * Get calculations for a specific account
         * 
         * GET
         * /api/interest/account/{accountNumber}?startDate=2024-01-01&endDate=2024-01-31
         * 
         * Requires: Any authenticated user
         */
        @GetMapping("/account/{accountNumber}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<List<InterestCalculation>> getAccountCalculations(
                        @PathVariable String accountNumber,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

                log.info("Retrieving interest calculations for account: {}", accountNumber);

                Account account = accountRepository.findByAccountNumber(accountNumber)
                                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));

                LocalDate defaultStart = startDate != null ? startDate : LocalDate.now().minusMonths(1);
                LocalDate defaultEnd = endDate != null ? endDate : LocalDate.now();

                List<InterestCalculation> calculations = interestCalculationRepository
                                .findByAccountIdAndCalculationDateBetween(
                                                account.getId(), defaultStart, defaultEnd);

                return ResponseEntity.ok(calculations);
        }
}
