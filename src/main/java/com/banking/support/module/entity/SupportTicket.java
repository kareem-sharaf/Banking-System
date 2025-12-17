package com.banking.support.module.entity;

import com.banking.core.enums.TicketStatus;
import com.banking.core.enums.TicketPriority;
import com.banking.core.enums.TicketCategory;
import com.banking.customer.module.entity.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Support Ticket Entity
 */
@Entity
@Table(name = "support_tickets", indexes = {
        @Index(name = "idx_ticket_number", columnList = "ticket_number"),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_date", columnList = "created_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "assigned_to_id")
    private Long assignedToId;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TicketCategory category;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;

    @Column(name = "resolution_notes", length = 2000)
    private String resolutionNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
