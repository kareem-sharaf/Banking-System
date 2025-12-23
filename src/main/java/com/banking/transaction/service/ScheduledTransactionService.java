package com.banking.transaction.service;

import com.banking.transaction.dto.CreateScheduledTransactionRequest;
import com.banking.transaction.dto.ScheduledTransactionResponse;
import com.banking.transaction.dto.UpdateScheduledTransactionRequest;
import com.banking.transaction.module.entity.ScheduledTransaction;
import com.banking.transaction.module.entity.Transaction;
import com.banking.transaction.repository.ScheduledTransactionRepository;
import com.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduled Transaction Service
 *
 * Service layer for Scheduled Transaction Management (CRUD).
 *
 * @author Banking System
 */
@Service
@RequiredArgsConstructor
public class ScheduledTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTransactionService.class);

    private final ScheduledTransactionRepository scheduledTransactionRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Create a new scheduled transaction
     */
    @Transactional
    public ScheduledTransactionResponse createScheduledTransaction(CreateScheduledTransactionRequest request) {
        logger.info("Creating scheduled transaction for template ID: {}", request.getTransactionTemplateId());

        // Validate transaction template exists
        Transaction transactionTemplate = transactionRepository.findById(request.getTransactionTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction template not found with ID: " + request.getTransactionTemplateId()));

        ScheduledTransaction scheduledTransaction = new ScheduledTransaction();
        scheduledTransaction.setTransactionTemplate(transactionTemplate);
        scheduledTransaction.setScheduleType(request.getScheduleType());
        scheduledTransaction.setNextExecutionDate(request.getNextExecutionDate());
        scheduledTransaction.setEndDate(request.getEndDate());
        scheduledTransaction.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        ScheduledTransaction savedScheduledTransaction = scheduledTransactionRepository.save(scheduledTransaction);
        logger.info("Created scheduled transaction with ID: {}", savedScheduledTransaction.getId());

        return mapToScheduledTransactionResponse(savedScheduledTransaction);
    }

    /**
     * Get all scheduled transactions
     */
    public List<ScheduledTransactionResponse> getAllScheduledTransactions() {
        logger.info("Retrieving all scheduled transactions");
        return scheduledTransactionRepository.findAll().stream()
                .map(this::mapToScheduledTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get scheduled transaction by ID
     */
    public ScheduledTransactionResponse getScheduledTransactionById(Long id) {
        logger.info("Retrieving scheduled transaction with ID: {}", id);
        ScheduledTransaction scheduledTransaction = scheduledTransactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled transaction not found with ID: " + id));
        return mapToScheduledTransactionResponse(scheduledTransaction);
    }

    /**
     * Get active scheduled transactions
     */
    public List<ScheduledTransactionResponse> getActiveScheduledTransactions() {
        logger.info("Retrieving active scheduled transactions");
        return scheduledTransactionRepository.findByIsActive(true).stream()
                .map(this::mapToScheduledTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get scheduled transactions by transaction template ID
     */
    public List<ScheduledTransactionResponse> getScheduledTransactionsByTemplateId(Long templateId) {
        logger.info("Retrieving scheduled transactions for template ID: {}", templateId);
        return scheduledTransactionRepository.findByTransactionTemplateId(templateId).stream()
                .map(this::mapToScheduledTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update scheduled transaction
     */
    @Transactional
    public ScheduledTransactionResponse updateScheduledTransaction(Long id, UpdateScheduledTransactionRequest request) {
        logger.info("Updating scheduled transaction with ID: {}", id);

        ScheduledTransaction scheduledTransaction = scheduledTransactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled transaction not found with ID: " + id));

        // Update fields if provided
        if (request.getScheduleType() != null) {
            scheduledTransaction.setScheduleType(request.getScheduleType());
        }
        if (request.getNextExecutionDate() != null) {
            scheduledTransaction.setNextExecutionDate(request.getNextExecutionDate());
        }
        if (request.getEndDate() != null) {
            scheduledTransaction.setEndDate(request.getEndDate());
        }
        if (request.getIsActive() != null) {
            scheduledTransaction.setIsActive(request.getIsActive());
        }

        ScheduledTransaction updatedScheduledTransaction = scheduledTransactionRepository.save(scheduledTransaction);
        logger.info("Updated scheduled transaction with ID: {}", updatedScheduledTransaction.getId());

        return mapToScheduledTransactionResponse(updatedScheduledTransaction);
    }

    /**
     * Delete scheduled transaction
     */
    @Transactional
    public void deleteScheduledTransaction(Long id) {
        logger.info("Deleting scheduled transaction with ID: {}", id);

        if (!scheduledTransactionRepository.existsById(id)) {
            throw new IllegalArgumentException("Scheduled transaction not found with ID: " + id);
        }

        scheduledTransactionRepository.deleteById(id);
        logger.info("Deleted scheduled transaction with ID: {}", id);
    }

    /**
     * Process due scheduled transactions
     * This would typically be called by a scheduler
     */
    @Transactional
    public void processDueScheduledTransactions() {
        logger.info("Processing due scheduled transactions");

        LocalDateTime now = LocalDateTime.now();
        List<ScheduledTransaction> dueTransactions = scheduledTransactionRepository.findDueScheduledTransactions(now);

        for (ScheduledTransaction scheduledTransaction : dueTransactions) {
            try {
                processScheduledTransaction(scheduledTransaction);
            } catch (Exception e) {
                logger.error("Error processing scheduled transaction {}: {}", scheduledTransaction.getId(), e.getMessage(), e);
            }
        }

        logger.info("Processed {} due scheduled transactions", dueTransactions.size());
    }

    /**
     * Process a single scheduled transaction
     */
    private void processScheduledTransaction(ScheduledTransaction scheduledTransaction) {
        logger.info("Processing scheduled transaction: {}", scheduledTransaction.getId());

        // Create a new transaction based on the template
        Transaction template = scheduledTransaction.getTransactionTemplate();
        Transaction newTransaction = new Transaction();
        newTransaction.setTransactionNumber(generateTransactionNumber());
        newTransaction.setAmount(template.getAmount());
        newTransaction.setTransactionType(template.getTransactionType());
        newTransaction.setFromAccount(template.getFromAccount());
        newTransaction.setToAccount(template.getToAccount());
        newTransaction.setDescription("Scheduled: " + template.getDescription());
        newTransaction.setStatus(com.banking.core.enums.TransactionStatus.PENDING);
        newTransaction.setTransactionDate(LocalDateTime.now());

        transactionRepository.save(newTransaction);

        // Update the scheduled transaction
        scheduledTransaction.setLastExecutedDate(LocalDateTime.now());
        scheduledTransaction.setExecutionCount(scheduledTransaction.getExecutionCount() + 1);
        scheduledTransaction.setNextExecutionDate(calculateNextExecutionDate(scheduledTransaction));

        // Check if the scheduled transaction should be deactivated
        if (scheduledTransaction.getEndDate() != null &&
            scheduledTransaction.getNextExecutionDate().isAfter(scheduledTransaction.getEndDate())) {
            scheduledTransaction.setIsActive(false);
        }

        scheduledTransactionRepository.save(scheduledTransaction);
    }

    /**
     * Calculate next execution date based on schedule type
     */
    private LocalDateTime calculateNextExecutionDate(ScheduledTransaction scheduledTransaction) {
        LocalDateTime current = scheduledTransaction.getNextExecutionDate();

        switch (scheduledTransaction.getScheduleType()) {
            case DAILY:
                return current.plusDays(1);
            case WEEKLY:
                return current.plusWeeks(1);
            case MONTHLY:
                return current.plusMonths(1);
            case YEARLY:
                return current.plusYears(1);
            default:
                return current.plusDays(1); // Default to daily
        }
    }

    private ScheduledTransactionResponse mapToScheduledTransactionResponse(ScheduledTransaction scheduledTransaction) {
        return ScheduledTransactionResponse.builder()
                .id(scheduledTransaction.getId())
                .transactionTemplateId(scheduledTransaction.getTransactionTemplate().getId())
                .scheduleType(scheduledTransaction.getScheduleType())
                .nextExecutionDate(scheduledTransaction.getNextExecutionDate())
                .endDate(scheduledTransaction.getEndDate())
                .isActive(scheduledTransaction.getIsActive())
                .lastExecutedDate(scheduledTransaction.getLastExecutedDate())
                .executionCount(scheduledTransaction.getExecutionCount())
                .createdAt(scheduledTransaction.getCreatedAt())
                .updatedAt(scheduledTransaction.getUpdatedAt())
                .transactionNumber(scheduledTransaction.getTransactionTemplate().getTransactionNumber())
                .description(scheduledTransaction.getTransactionTemplate().getDescription())
                .build();
    }

    private String generateTransactionNumber() {
        return "TXN-" + System.currentTimeMillis();
    }
}
