package com.banking.support.service;

import com.banking.support.dto.CreateSupportTicketRequest;
import com.banking.support.dto.SupportTicketResponse;
import com.banking.support.dto.UpdateSupportTicketRequest;
import com.banking.support.module.entity.SupportTicket;
import com.banking.support.repository.SupportTicketRepository;
import com.banking.customer.repository.CustomerRepository;
import com.banking.customer.module.entity.Customer;
import com.banking.core.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Support Ticket Service
 *
 * Service layer for Support Ticket Management (CRUD).
 *
 * @author Banking System
 */
@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketService.class);

    private final SupportTicketRepository supportTicketRepository;
    private final CustomerRepository customerRepository;

    /**
     * Create a new support ticket
     */
    @Transactional
    public SupportTicketResponse createSupportTicket(CreateSupportTicketRequest request, Long customerId) {
        logger.info("Creating support ticket for customer: {}", customerId);

        // Validate customer exists
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));

        SupportTicket supportTicket = new SupportTicket();
        supportTicket.setTicketNumber(generateTicketNumber());
        supportTicket.setCustomer(customer);
        supportTicket.setSubject(request.getSubject());
        supportTicket.setDescription(request.getDescription());
        supportTicket.setCategory(request.getCategory());
        supportTicket.setPriority(request.getPriority());
        supportTicket.setStatus(TicketStatus.OPEN);

        SupportTicket savedTicket = supportTicketRepository.save(supportTicket);
        logger.info("Created support ticket with ID: {}", savedTicket.getId());

        return mapToSupportTicketResponse(savedTicket);
    }

    /**
     * Get all support tickets
     */
    public List<SupportTicketResponse> getAllSupportTickets() {
        logger.info("Retrieving all support tickets");
        return supportTicketRepository.findAll().stream()
                .map(this::mapToSupportTicketResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get support ticket by ID
     */
    public SupportTicketResponse getSupportTicketById(Long id) {
        logger.info("Retrieving support ticket with ID: {}", id);
        SupportTicket supportTicket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found with ID: " + id));
        return mapToSupportTicketResponse(supportTicket);
    }

    /**
     * Get support ticket by ticket number
     */
    public SupportTicketResponse getSupportTicketByNumber(String ticketNumber) {
        logger.info("Retrieving support ticket with number: {}", ticketNumber);
        SupportTicket supportTicket = supportTicketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found with number: " + ticketNumber));
        return mapToSupportTicketResponse(supportTicket);
    }

    /**
     * Get support tickets by customer ID
     */
    public List<SupportTicketResponse> getSupportTicketsByCustomerId(Long customerId) {
        logger.info("Retrieving support tickets for customer: {}", customerId);
        return supportTicketRepository.findByCustomerId(customerId).stream()
                .map(this::mapToSupportTicketResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get support tickets by status
     */
    public List<SupportTicketResponse> getSupportTicketsByStatus(TicketStatus status) {
        logger.info("Retrieving support tickets with status: {}", status);
        return supportTicketRepository.findByStatus(status).stream()
                .map(this::mapToSupportTicketResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get support tickets assigned to a user
     */
    public List<SupportTicketResponse> getSupportTicketsByAssignee(Long assignedToId) {
        logger.info("Retrieving support tickets assigned to user: {}", assignedToId);
        return supportTicketRepository.findByAssignedToId(assignedToId).stream()
                .map(this::mapToSupportTicketResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update support ticket
     */
    @Transactional
    public SupportTicketResponse updateSupportTicket(Long id, UpdateSupportTicketRequest request) {
        logger.info("Updating support ticket with ID: {}", id);

        SupportTicket supportTicket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found with ID: " + id));

        // Update fields if provided
        if (request.getStatus() != null) {
            supportTicket.setStatus(request.getStatus());

            // Set resolved date if status is RESOLVED or CLOSED
            if (request.getStatus() == TicketStatus.RESOLVED || request.getStatus() == TicketStatus.CLOSED) {
                if (supportTicket.getResolvedDate() == null) {
                    supportTicket.setResolvedDate(LocalDateTime.now());
                }
            } else {
                supportTicket.setResolvedDate(null);
            }
        }

        if (request.getPriority() != null) {
            supportTicket.setPriority(request.getPriority());
        }

        if (request.getAssignedToId() != null) {
            supportTicket.setAssignedToId(request.getAssignedToId());
        }

        if (request.getResolutionNotes() != null) {
            supportTicket.setResolutionNotes(request.getResolutionNotes());
        }

        SupportTicket updatedTicket = supportTicketRepository.save(supportTicket);
        logger.info("Updated support ticket with ID: {}", updatedTicket.getId());

        return mapToSupportTicketResponse(updatedTicket);
    }

    /**
     * Assign ticket to user
     */
    @Transactional
    public SupportTicketResponse assignTicket(Long ticketId, Long assignedToId) {
        logger.info("Assigning ticket {} to user {}", ticketId, assignedToId);

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found with ID: " + ticketId));

        supportTicket.setAssignedToId(assignedToId);
        SupportTicket updatedTicket = supportTicketRepository.save(supportTicket);

        logger.info("Assigned ticket {} to user {}", ticketId, assignedToId);
        return mapToSupportTicketResponse(updatedTicket);
    }

    /**
     * Resolve ticket
     */
    @Transactional
    public SupportTicketResponse resolveTicket(Long ticketId, String resolutionNotes) {
        logger.info("Resolving ticket {}", ticketId);

        SupportTicket supportTicket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found with ID: " + ticketId));

        supportTicket.setStatus(TicketStatus.RESOLVED);
        supportTicket.setResolvedDate(LocalDateTime.now());
        supportTicket.setResolutionNotes(resolutionNotes);

        SupportTicket updatedTicket = supportTicketRepository.save(supportTicket);

        logger.info("Resolved ticket {}", ticketId);
        return mapToSupportTicketResponse(updatedTicket);
    }

    /**
     * Delete support ticket
     */
    @Transactional
    public void deleteSupportTicket(Long id) {
        logger.info("Deleting support ticket with ID: {}", id);

        if (!supportTicketRepository.existsById(id)) {
            throw new IllegalArgumentException("Support ticket not found with ID: " + id);
        }

        supportTicketRepository.deleteById(id);
        logger.info("Deleted support ticket with ID: {}", id);
    }

    /**
     * Check if ticket number exists
     */
    public boolean existsByTicketNumber(String ticketNumber) {
        return supportTicketRepository.existsByTicketNumber(ticketNumber);
    }

    private SupportTicketResponse mapToSupportTicketResponse(SupportTicket supportTicket) {
        return SupportTicketResponse.builder()
                .id(supportTicket.getId())
                .ticketNumber(supportTicket.getTicketNumber())
                .customerId(supportTicket.getCustomer() != null ? supportTicket.getCustomer().getId() : null)
                .customerName(buildCustomerName(supportTicket.getCustomer()))
                .assignedToId(supportTicket.getAssignedToId())
                .subject(supportTicket.getSubject())
                .description(supportTicket.getDescription())
                .status(supportTicket.getStatus())
                .priority(supportTicket.getPriority())
                .category(supportTicket.getCategory())
                .createdDate(supportTicket.getCreatedDate())
                .resolvedDate(supportTicket.getResolvedDate())
                .resolutionNotes(supportTicket.getResolutionNotes())
                .createdAt(supportTicket.getCreatedAt())
                .updatedAt(supportTicket.getUpdatedAt())
                .build();
    }

    private String buildCustomerName(Customer customer) {
        if (customer == null || customer.getUser() == null) {
            return "Unknown";
        }
        return String.format("%s %s",
            customer.getUser().getFirstName(),
            customer.getUser().getLastName());
    }

    private String generateTicketNumber() {
        return "TICKET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
