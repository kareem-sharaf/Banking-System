package com.banking.support.dto;

import com.banking.core.enums.TicketCategory;
import com.banking.core.enums.TicketPriority;
import com.banking.core.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Support Ticket Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketResponse {
    private Long id;
    private String ticketNumber;
    private Long customerId;
    private String customerName;
    private Long assignedToId;
    private String assignedToName;
    private String subject;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private TicketCategory category;
    private LocalDateTime createdDate;
    private LocalDateTime resolvedDate;
    private String resolutionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
