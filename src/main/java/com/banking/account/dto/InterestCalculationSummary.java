package com.banking.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Interest Calculation Summary DTO
 * 
 * @author Banking System
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestCalculationSummary {

    private LocalDate calculationDate;
    private Integer totalAccounts;
    private Integer successfulCalculations;
    private Integer failedCalculations;
    private Integer skippedAccounts;
    private BigDecimal totalInterestCalculated;
    private List<AccountCalculationDetail> accountDetails;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccountCalculationDetail {
        private String accountNumber;
        private String accountType;
        private BigDecimal interestAmount;
        private BigDecimal previousBalance;
        private BigDecimal newBalance;
        private String status;
        private String errorMessage;
    }
}
