package com.banking.repository;

import com.banking.entity.SupportTicket;
import com.banking.enums.TicketStatus;
import com.banking.enums.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    List<SupportTicket> findByCustomerId(Long customerId);

    List<SupportTicket> findByStatus(TicketStatus status);

    List<SupportTicket> findByPriority(TicketPriority priority);

    List<SupportTicket> findByAssignedToId(Long assignedToId);

    List<SupportTicket> findByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    boolean existsByTicketNumber(String ticketNumber);
}
