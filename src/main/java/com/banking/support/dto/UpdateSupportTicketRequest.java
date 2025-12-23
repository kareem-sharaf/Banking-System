package com.banking.support.dto;

import com.banking.core.enums.TicketPriority;
import com.banking.core.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update Support Ticket Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupportTicketRequest {

    private TicketStatus status;

    private TicketPriority priority;

    private Long assignedToId;

    private String resolutionNotes;
}
