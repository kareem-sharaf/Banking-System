package com.banking.core.repository;

import com.banking.core.module.entity.AuditLog;
import com.banking.core.enums.ActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByAccountId(Long accountId);

    List<AuditLog> findByTransactionId(Long transactionId);

    List<AuditLog> findByActionType(ActionType actionType);

    List<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT al FROM AuditLog al WHERE al.account.id = :accountId ORDER BY al.timestamp DESC")
    List<AuditLog> findAllByAccountIdOrderByTimestampDesc(@Param("accountId") Long accountId);
}
