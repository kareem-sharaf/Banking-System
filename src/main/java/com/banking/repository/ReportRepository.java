package com.banking.repository;

import com.banking.entity.Report;
import com.banking.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByReportType(ReportType reportType);
    List<Report> findByGeneratedById(Long generatedById);
    List<Report> findByGeneratedDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT r FROM Report r WHERE r.reportType = :reportType AND r.generatedDate BETWEEN :startDate AND :endDate")
    List<Report> findByReportTypeAndDateRange(@Param("reportType") ReportType reportType, 
                                               @Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
}

