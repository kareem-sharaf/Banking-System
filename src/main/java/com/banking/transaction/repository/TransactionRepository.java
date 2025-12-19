package com.banking.transaction.repository;

import com.banking.transaction.module.entity.Transaction;
import com.banking.core.enums.TransactionStatus;
import com.banking.core.enums.TransactionType;
import com.banking.core.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionNumber(String transactionNumber);

    List<Transaction> findByFromAccountId(Long fromAccountId);

    List<Transaction> findByToAccountId(Long toAccountId);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByTransactionType(TransactionType transactionType);

    List<Transaction> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Transaction> findByFromAccountIdAndTransactionDateBetween(Long accountId, LocalDateTime startDate,
            LocalDateTime endDate);

    boolean existsByTransactionNumber(String transactionNumber);

    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.id = :accountId OR t.toAccount.id = :accountId")
    List<Transaction> findAllByAccountId(@Param("accountId") Long accountId);

    /**
     * Find transactions by approval status
     */
    List<Transaction> findByApprovalStatus(ApprovalStatus approvalStatus);

    /**
     * Find pending approval transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.approvalStatus = :status ORDER BY t.transactionDate DESC")
    List<Transaction> findPendingApprovals(@Param("status") ApprovalStatus status);

    /**
     * Find transactions by approval status and transaction type
     */
    List<Transaction> findByApprovalStatusAndTransactionType(ApprovalStatus approvalStatus,
            TransactionType transactionType);
}
