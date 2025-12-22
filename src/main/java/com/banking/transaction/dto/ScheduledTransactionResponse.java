package com.banking.transaction.dto;

import com.banking.core.enums.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Scheduled Transaction Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTransactionResponse {
    private Long id;
    private Long transactionTemplateId;
    private ScheduleType scheduleType;
    private LocalDateTime nextExecutionDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private LocalDateTime lastExecutedDate;
    private Integer executionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transaction template details
    private String transactionNumber;
    private String description;
}
