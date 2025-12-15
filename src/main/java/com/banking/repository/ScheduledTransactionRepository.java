package com.banking.repository;

import com.banking.entity.ScheduledTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledTransactionRepository extends JpaRepository<ScheduledTransaction, Long> {
    List<ScheduledTransaction> findByIsActive(Boolean isActive);
    
    @Query("SELECT st FROM ScheduledTransaction st WHERE st.isActive = true AND st.nextExecutionDate <= :currentDate AND (st.endDate IS NULL OR st.endDate >= :currentDate)")
    List<ScheduledTransaction> findDueScheduledTransactions(@Param("currentDate") LocalDateTime currentDate);
    
    List<ScheduledTransaction> findByTransactionTemplateId(Long transactionTemplateId);
}

