package com.banking.transaction.controller;

import com.banking.core.enums.ApprovalStatus;
import com.banking.transaction.dto.ApprovalRequest;
import com.banking.transaction.dto.ApprovalResponse;
import com.banking.transaction.module.entity.Transaction;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.service.TransactionApprovalService;
import com.banking.transaction.approval.ApprovalResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Transaction Approval Controller
 * 
 * REST API endpoints for transaction approval workflow.
 * Allows managers and directors to approve/reject pending transactions.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionApprovalController {

        private final TransactionApprovalService transactionApprovalService;
        private final TransactionRepository transactionRepository;

        /**
         * Approve a pending transaction
         * 
         * POST /api/transactions/{transactionId}/approve
         * 
         * Requires: MANAGER or ADMIN role
         */
        @PostMapping("/{transactionId}/approve")
        @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
        public ResponseEntity<ApprovalResponse> approveTransaction(
                        @PathVariable Long transactionId,
                        @Valid @RequestBody(required = false) ApprovalRequest request) {

                log.info("Approval request received for transaction ID: {}", transactionId);

                // Get current user ID from security context
                Long approverId = getCurrentUserId();
                if (approverId == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(ApprovalResponse.builder()
                                                        .success(false)
                                                        .message("User not authenticated")
                                                        .build());
                }

                String comment = request != null && request.getComment() != null
                                ? request.getComment()
                                : "Approved by manager";

                try {
                        ApprovalResult result = transactionApprovalService.approveTransaction(
                                        transactionId, approverId, comment);

                        ApprovalResponse response = ApprovalResponse.builder()
                                        .success(true)
                                        .message(result.getMessage())
                                        .approvalStatus(result.getStatus())
                                        .transactionId(transactionId)
                                        .transactionNumber(result.getTransaction().getTransactionNumber())
                                        .approvedBy(approverId)
                                        .approvedAt(result.getApprovedAt())
                                        .comment(result.getComment())
                                        .handlerUsed(result.getHandlerUsed())
                                        .build();

                        return ResponseEntity.ok(response);

                } catch (IllegalArgumentException e) {
                        log.error("Invalid request: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApprovalResponse.builder()
                                                        .success(false)
                                                        .message(e.getMessage())
                                                        .transactionId(transactionId)
                                                        .build());

                } catch (IllegalStateException e) {
                        log.error("Invalid state: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(ApprovalResponse.builder()
                                                        .success(false)
                                                        .message(e.getMessage())
                                                        .transactionId(transactionId)
                                                        .build());

                } catch (Exception e) {
                        log.error("Error approving transaction: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(ApprovalResponse.builder()
                                                        .success(false)
                                                        .message("Error approving transaction: " + e.getMessage())
                                                        .transactionId(transactionId)
                                                        .build());
                }
        }

        /**
         * Reject a pending transaction
         * 
         * POST /api/transactions/{transactionId}/reject
         * 
         * Requires: MANAGER or ADMIN role
         */
        @PostMapping("/{transactionId}/reject")
        @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
        public ResponseEntity<ApprovalResponse> rejectTransaction(
                        @PathVariable Long transactionId,
                        @Valid @RequestBody ApprovalRequest request) {

                log.info("Rejection request received for transaction ID: {}", transactionId);

                // Get current user ID from security context
                Long rejecterId = getCurrentUserId();
                if (rejecterId == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(ApprovalResponse.builder()
                                                        .success(false)
                                                        .message("User not authenticated")
                                                        .build());
                }

                try {
                        ApprovalResult result = transactionApprovalService.rejectTransaction(
                                        transactionId, rejecterId, request.getComment());

                        ApprovalResponse response = ApprovalResponse.builder()
                                        .success(true)
                                        .message(result.getMessage())
                                        .approvalStatus(result.getStatus())
                                        .transactionId(transactionId)
                                        .transactionNumber(result.getTransaction().getTransactionNumber())
                                        .approvedBy(rejecterId)
                                        .approvedAt(result.getRejectedAt())
                                        .comment(result.getComment())
                                        .handlerUsed(result.getHandlerUsed())
                                        .build();

                        return ResponseEntity.ok(response);

                } catch (IllegalArgumentException e) {
                        log.error("Invalid request: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApprovalResponse.builder()
                                                        .success(false)
                                                        .message(e.getMessage())
                                                        .transactionId(transactionId)
                                                        .build());

                } catch (IllegalStateException e) {
                        log.error("Invalid state: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(ApprovalResponse.builder()
                                                        .success(false)
                                                        .message(e.getMessage())
                                                        .transactionId(transactionId)
                                                        .build());

                } catch (Exception e) {
                        log.error("Error rejecting transaction: {}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(ApprovalResponse.builder()
                                                        .success(false)
                                                        .message("Error rejecting transaction: " + e.getMessage())
                                                        .transactionId(transactionId)
                                                        .build());
                }
        }

        /**
         * Get all pending approval transactions
         * 
         * GET /api/transactions/pending-approvals
         * 
         * Requires: MANAGER or ADMIN role
         */
        @GetMapping("/pending-approvals")
        @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
        public ResponseEntity<List<Transaction>> getPendingApprovals() {
                log.info("Retrieving pending approval transactions");

                List<Transaction> pendingTransactions = transactionRepository
                                .findByApprovalStatus(ApprovalStatus.PENDING);

                return ResponseEntity.ok(pendingTransactions);
        }

        /**
         * Get approval status of a transaction
         * 
         * GET /api/transactions/{transactionId}/approval-status
         * 
         * Requires: Any authenticated user
         */
        @GetMapping("/{transactionId}/approval-status")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApprovalResponse> getApprovalStatus(@PathVariable Long transactionId) {
                log.info("Retrieving approval status for transaction ID: {}", transactionId);

                Transaction transaction = transactionRepository.findById(transactionId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Transaction not found: " + transactionId));

                ApprovalResponse response = ApprovalResponse.builder()
                                .success(true)
                                .message("Approval status retrieved")
                                .approvalStatus(transaction.getApprovalStatus())
                                .transactionId(transactionId)
                                .transactionNumber(transaction.getTransactionNumber())
                                .approvedBy(transaction.getApprovedBy())
                                .approvedAt(transaction.getApprovedAt())
                                .comment(transaction.getApprovalComment())
                                .handlerUsed(transaction.getHandlerUsed())
                                .build();

                return ResponseEntity.ok(response);
        }

        /**
         * Get current user ID from security context
         * TODO: Implement based on your User entity structure
         */
        private Long getCurrentUserId() {
                try {
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                                // Extract user ID from authentication
                                // This is a placeholder - adjust based on your authentication implementation
                                // For Keycloak JWT, you might extract it from token claims
                                // For now, return null and handle in the service layer
                                return null;
                        }
                } catch (Exception e) {
                        log.warn("Could not get current user ID: {}", e.getMessage());
                }
                return null;
        }
}
