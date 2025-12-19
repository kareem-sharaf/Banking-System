package com.banking.transaction.service;

import com.banking.core.enums.ApprovalStatus;
import com.banking.core.enums.TransactionStatus;
import com.banking.transaction.approval.ApprovalContext;
import com.banking.transaction.approval.ApprovalHandler;
import com.banking.transaction.approval.ApprovalResult;
import com.banking.transaction.module.entity.Transaction;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Transaction Approval Service
 * 
 * Handles transaction approval workflow using Chain of Responsibility pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionApprovalService {

    private final ApprovalHandler approvalChain;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    /**
     * Process approval for a transaction through the approval chain
     * 
     * @param transaction The transaction to process
     * @return ApprovalResult with the outcome
     */
    @Transactional
    public ApprovalResult processApproval(Transaction transaction) {
        log.info("Processing approval for transaction: {}", transaction.getTransactionNumber());

        // Create approval context
        ApprovalContext context = createContext(transaction);

        // Process through the approval chain
        ApprovalResult result = approvalChain.handle(context);

        // Update transaction with approval result
        updateTransactionWithApprovalResult(transaction, result);

        // Save transaction
        transactionRepository.save(transaction);

        log.info("Approval processed for transaction {}: status={}, handler={}",
                transaction.getTransactionNumber(),
                result.getStatus(),
                result.getHandlerUsed());

        return result;
    }

    /**
     * Manually approve a transaction (by manager or director)
     * 
     * @param transactionId The transaction ID to approve
     * @param approverId    The ID of the user approving
     * @param comment       Optional comment
     * @return ApprovalResult
     */
    @Transactional
    public ApprovalResult approveTransaction(Long transactionId, Long approverId, String comment) {
        log.info("Manual approval requested for transaction ID: {} by user: {}", transactionId, approverId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (transaction.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("Transaction %s is not pending approval. Current status: %s",
                            transaction.getTransactionNumber(), transaction.getApprovalStatus()));
        }

        // Update transaction
        transaction.setApprovalStatus(ApprovalStatus.APPROVED);
        transaction.setApprovedBy(approverId);
        transaction.setApprovedAt(LocalDateTime.now());
        transaction.setApprovalComment(comment);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setUpdatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);

        log.info("Transaction {} approved by user {}", transaction.getTransactionNumber(), approverId);

        return ApprovalResult.builder()
                .status(ApprovalStatus.APPROVED)
                .transaction(transaction)
                .approvedBy(approverId)
                .approvedAt(LocalDateTime.now())
                .comment(comment)
                .message("Transaction approved successfully")
                .build();
    }

    /**
     * Manually reject a transaction
     * 
     * @param transactionId The transaction ID to reject
     * @param rejecterId    The ID of the user rejecting
     * @param reason        Rejection reason
     * @return ApprovalResult
     */
    @Transactional
    public ApprovalResult rejectTransaction(Long transactionId, Long rejecterId, String reason) {
        log.info("Manual rejection requested for transaction ID: {} by user: {}", transactionId, rejecterId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (transaction.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("Transaction %s is not pending approval. Current status: %s",
                            transaction.getTransactionNumber(), transaction.getApprovalStatus()));
        }

        // Update transaction
        transaction.setApprovalStatus(ApprovalStatus.REJECTED);
        transaction.setRejectedBy(rejecterId);
        transaction.setRejectedAt(LocalDateTime.now());
        transaction.setApprovalComment(reason);
        transaction.setStatus(TransactionStatus.REJECTED);
        transaction.setUpdatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);

        log.info("Transaction {} rejected by user {}", transaction.getTransactionNumber(), rejecterId);

        return ApprovalResult.builder()
                .status(ApprovalStatus.REJECTED)
                .transaction(transaction)
                .rejectedBy(rejecterId)
                .rejectedAt(LocalDateTime.now())
                .comment(reason)
                .message("Transaction rejected")
                .build();
    }

    /**
     * Create approval context from transaction
     */
    private ApprovalContext createContext(Transaction transaction) {
        // Get current user ID if available
        Long requesterId = getCurrentUserId();
        String requesterRole = getCurrentUserRole();

        return ApprovalContext.builder()
                .transaction(transaction)
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .requesterId(requesterId)
                .requesterRole(requesterRole)
                .fromAccount(transaction.getFromAccount())
                .toAccount(transaction.getToAccount())
                .description(transaction.getDescription())
                .build();
    }

    /**
     * Update transaction with approval result
     */
    private void updateTransactionWithApprovalResult(Transaction transaction, ApprovalResult result) {
        transaction.setApprovalStatus(result.getStatus());
        transaction.setHandlerUsed(result.getHandlerUsed());
        transaction.setApprovalComment(result.getComment());

        if (result.isApproved()) {
            // Auto-approved transactions are immediately completed
            if (result.getStatus() == ApprovalStatus.AUTO_APPROVED) {
                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setApprovedAt(result.getApprovedAt());
            } else {
                // Manually approved - status already set
                transaction.setApprovedBy(result.getApprovedBy());
                transaction.setApprovedAt(result.getApprovedAt());
            }
        } else if (result.isPending()) {
            // Keep as PENDING - waiting for manual approval
            transaction.setStatus(TransactionStatus.PENDING);
        } else if (result.isRejected()) {
            transaction.setStatus(TransactionStatus.REJECTED);
            transaction.setRejectedBy(result.getRejectedBy());
            transaction.setRejectedAt(result.getRejectedAt());
        }

        transaction.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Get current user ID from security context
     */
    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                // Extract user ID from authentication (adjust based on your auth
                // implementation)
                // This is a placeholder - you may need to adjust based on your User entity
                // structure
                return null; // TODO: Implement based on your User entity
            }
        } catch (Exception e) {
            log.warn("Could not get current user ID: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get current user role from security context
     */
    private String getCurrentUserRole() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getAuthorities() != null) {
                return auth.getAuthorities().iterator().next().getAuthority();
            }
        } catch (Exception e) {
            log.warn("Could not get current user role: {}", e.getMessage());
        }
        return null;
    }
}
