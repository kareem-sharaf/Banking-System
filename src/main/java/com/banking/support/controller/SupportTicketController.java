package com.banking.support.controller;

import com.banking.core.enums.TicketStatus;
import com.banking.support.dto.CreateSupportTicketRequest;
import com.banking.support.dto.SupportTicketResponse;
import com.banking.support.dto.UpdateSupportTicketRequest;
import com.banking.support.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Support Ticket Controller
 *
 * REST API for Support Ticket Management (CRUD).
 * Handles customer inquiries and support ticket operations.
 *
 * @author Banking System
 */
@RestController
@RequestMapping("/api/support-tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketController.class);

    private final SupportTicketService supportTicketService;

    /**
     * Create a new support ticket
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<SupportTicketResponse> createSupportTicket(@Valid @RequestBody CreateSupportTicketRequest request) {
        logger.info("Received request to create support ticket: {}", request.getSubject());

        // Get current user ID (customer)
        // In a real implementation, you'd extract customer ID from JWT token
        Long customerId = 1L; // Placeholder - should be extracted from JWT token

        SupportTicketResponse response = supportTicketService.createSupportTicket(request, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all support tickets (Admin/Teller/Manager only)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<SupportTicketResponse>> getAllSupportTickets() {
        return ResponseEntity.ok(supportTicketService.getAllSupportTickets());
    }

    /**
     * Get support ticket by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<SupportTicketResponse> getSupportTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(supportTicketService.getSupportTicketById(id));
    }

    /**
     * Get support ticket by ticket number
     */
    @GetMapping("/number/{ticketNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<SupportTicketResponse> getSupportTicketByNumber(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(supportTicketService.getSupportTicketByNumber(ticketNumber));
    }

    /**
     * Get support tickets for current customer
     */
    @GetMapping("/my-tickets")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<SupportTicketResponse>> getMySupportTickets() {
        // In a real implementation, extract customer ID from JWT token
        Long customerId = 1L; // Placeholder
        return ResponseEntity.ok(supportTicketService.getSupportTicketsByCustomerId(customerId));
    }

    /**
     * Get support tickets by customer ID
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<SupportTicketResponse>> getSupportTicketsByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(supportTicketService.getSupportTicketsByCustomerId(customerId));
    }

    /**
     * Get support tickets by status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<SupportTicketResponse>> getSupportTicketsByStatus(@PathVariable TicketStatus status) {
        return ResponseEntity.ok(supportTicketService.getSupportTicketsByStatus(status));
    }

    /**
     * Get support tickets assigned to current user
     */
    @GetMapping("/assigned-to-me")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<SupportTicketResponse>> getTicketsAssignedToMe() {
        // In a real implementation, extract user ID from JWT token
        Long userId = 2L; // Placeholder for assigned user
        return ResponseEntity.ok(supportTicketService.getSupportTicketsByAssignee(userId));
    }

    /**
     * Update support ticket
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<SupportTicketResponse> updateSupportTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupportTicketRequest request) {
        return ResponseEntity.ok(supportTicketService.updateSupportTicket(id, request));
    }

    /**
     * Assign ticket to user
     */
    @PostMapping("/{id}/assign/{assignedToId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SupportTicketResponse> assignTicket(
            @PathVariable Long ticketId,
            @PathVariable Long assignedToId) {
        return ResponseEntity.ok(supportTicketService.assignTicket(ticketId, assignedToId));
    }

    /**
     * Resolve ticket
     */
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<SupportTicketResponse> resolveTicket(
            @PathVariable Long ticketId,
            @RequestBody(required = false) String resolutionNotes) {
        return ResponseEntity.ok(supportTicketService.resolveTicket(ticketId, resolutionNotes));
    }

    /**
     * Delete support ticket
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSupportTicket(@PathVariable Long id) {
        supportTicketService.deleteSupportTicket(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if ticket number exists
     */
    @GetMapping("/exists/number/{ticketNumber}")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Boolean> existsByTicketNumber(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(supportTicketService.existsByTicketNumber(ticketNumber));
    }

    /**
     * Get ticket statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Object> getTicketStatistics() {
        List<SupportTicketResponse> allTickets = supportTicketService.getAllSupportTickets();

        long openTickets = allTickets.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.OPEN)
                .count();

        long inProgressTickets = allTickets.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.IN_PROGRESS)
                .count();

        long resolvedTickets = allTickets.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.RESOLVED)
                .count();

        var stats = new java.util.HashMap<String, Object>();
        stats.put("totalTickets", allTickets.size());
        stats.put("openTickets", openTickets);
        stats.put("inProgressTickets", inProgressTickets);
        stats.put("resolvedTickets", resolvedTickets);
        stats.put("resolutionRate", allTickets.isEmpty() ? 0.0 :
                (double) resolvedTickets / allTickets.size() * 100.0);

        return ResponseEntity.ok(stats);
    }
}
