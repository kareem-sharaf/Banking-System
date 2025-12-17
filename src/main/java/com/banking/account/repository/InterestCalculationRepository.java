package com.banking.account.repository;

import com.banking.account.module.entity.InterestCalculation;
import com.banking.core.enums.CalculationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Interest Calculation Repository
 * 
 * @author Banking System
 */
@Repository
public interface InterestCalculationRepository extends JpaRepository<InterestCalculation, Long> {

    /**
     * Find all calculations for a specific date
     */
    List<InterestCalculation> findByCalculationDate(LocalDate date);

    /**
     * Find all calculations for an account within a date range
     */
    List<InterestCalculation> findByAccountIdAndCalculationDateBetween(
            Long accountId, LocalDate startDate, LocalDate endDate);

    /**
     * Find calculations by status
     */
    List<InterestCalculation> findByStatus(CalculationStatus status);

    /**
     * Find calculations by account and status
     */
    List<InterestCalculation> findByAccountIdAndStatus(Long accountId, CalculationStatus status);

    /**
     * Find the last calculation for an account
     */
    @Query("SELECT ic FROM InterestCalculation ic WHERE ic.account.id = :accountId ORDER BY ic.calculationDate DESC, ic.createdAt DESC")
    List<InterestCalculation> findLastCalculationByAccountId(@Param("accountId") Long accountId);

    /**
     * Count calculations by date and status
     */
    long countByCalculationDateAndStatus(LocalDate date, CalculationStatus status);

    /**
     * Find all calculations for a date range
     */
    List<InterestCalculation> findByCalculationDateBetween(LocalDate startDate, LocalDate endDate);
}
