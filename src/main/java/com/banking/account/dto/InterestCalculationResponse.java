package com.banking.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Interest Calculation Response DTO
 * 
 * @author Banking System
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestCalculationResponse {

    private Boolean success;
    private String message;
    private LocalDateTime calculationTime;
    private Integer totalAccounts;
    private Integer successfulCalculations;
    private Integer failedCalculations;
    private Integer skippedAccounts;
    private BigDecimal totalInterestCalculated;
    private LocalDate calculationDate;
}
