package com.banking.transaction.controller;

import com.banking.transaction.dto.CreateScheduledTransactionRequest;
import com.banking.transaction.dto.ScheduledTransactionResponse;
import com.banking.transaction.dto.UpdateScheduledTransactionRequest;
import com.banking.transaction.service.ScheduledTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Scheduled Transaction Controller
 *
 * REST API for Scheduled Transaction Management (CRUD).
 * Supports recurring payments and automated transactions.
 *
 * @author Banking System
 */
@RestController
@RequestMapping("/api/scheduled-transactions")
@RequiredArgsConstructor
public class ScheduledTransactionController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTransactionController.class);

    private final ScheduledTransactionService scheduledTransactionService;

    /**
     * Create a new scheduled transaction
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ScheduledTransactionResponse> createScheduledTransaction(@Valid @RequestBody CreateScheduledTransactionRequest request) {
        logger.info("Received request to create scheduled transaction");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduledTransactionService.createScheduledTransaction(request));
    }

    /**
     * Get all scheduled transactions
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<ScheduledTransactionResponse>> getAllScheduledTransactions() {
        return ResponseEntity.ok(scheduledTransactionService.getAllScheduledTransactions());
    }

    /**
     * Get scheduled transaction by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ScheduledTransactionResponse> getScheduledTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduledTransactionService.getScheduledTransactionById(id));
    }

    /**
     * Get active scheduled transactions
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<ScheduledTransactionResponse>> getActiveScheduledTransactions() {
        return ResponseEntity.ok(scheduledTransactionService.getActiveScheduledTransactions());
    }

    /**
     * Get scheduled transactions by transaction template ID
     */
    @GetMapping("/template/{templateId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<ScheduledTransactionResponse>> getScheduledTransactionsByTemplateId(@PathVariable Long templateId) {
        return ResponseEntity.ok(scheduledTransactionService.getScheduledTransactionsByTemplateId(templateId));
    }

    /**
     * Update scheduled transaction
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ScheduledTransactionResponse> updateScheduledTransaction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateScheduledTransactionRequest request) {
        return ResponseEntity.ok(scheduledTransactionService.updateScheduledTransaction(id, request));
    }

    /**
     * Delete scheduled transaction
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteScheduledTransaction(@PathVariable Long id) {
        scheduledTransactionService.deleteScheduledTransaction(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Manually process due scheduled transactions
     * This endpoint allows manual triggering of scheduled transaction processing
     */
    @PostMapping("/process-due")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> processDueScheduledTransactions() {
        logger.info("Manually processing due scheduled transactions");
        scheduledTransactionService.processDueScheduledTransactions();
        return ResponseEntity.ok("Due scheduled transactions processed successfully");
    }
}
