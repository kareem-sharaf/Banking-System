package com.banking.transaction.dto;

import com.banking.core.enums.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Update Scheduled Transaction Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateScheduledTransactionRequest {

    private ScheduleType scheduleType;

    private LocalDateTime nextExecutionDate;

    private LocalDateTime endDate;

    private Boolean isActive;
}
