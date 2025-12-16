package com.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction Response DTO
 * 
 * Data Transfer Object for transaction responses.
 * Contains information about the transaction result.
 * 
 * @author Banking System
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

   
    private Boolean success;

    
    private String message;

   
    private BigDecimal newBalance;

   
    private String transactionNumber;

   
    private String accountNumber;

   
    private LocalDateTime timestamp;

   
    private String currency;

   
    private BigDecimal amount;
}
