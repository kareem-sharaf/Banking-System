package com.banking.customer.facade.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionSummaryDto {
    private String transactionNumber;
    private String type;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime date;
    private String status;
    private String description;
}
