package com.banking.transaction.dto;

import com.banking.core.enums.ScheduleType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Create Scheduled Transaction Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateScheduledTransactionRequest {

    @NotNull(message = "Transaction template ID is required")
    private Long transactionTemplateId;

    @NotNull(message = "Schedule type is required")
    private ScheduleType scheduleType;

    @NotNull(message = "Next execution date is required")
    private LocalDateTime nextExecutionDate;

    private LocalDateTime endDate;

    @NotNull(message = "Active status is required")
    private Boolean isActive = true;
}
