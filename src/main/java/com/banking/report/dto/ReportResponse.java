package com.banking.report.dto;

import com.banking.core.enums.ReportFormat;
import com.banking.core.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Report Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private Long id;
    private ReportType reportType;
    private String reportTypeDescription;
    private Long generatedById;
    private String generatedByUsername;
    private LocalDateTime generatedDate;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String filePath;
    private ReportFormat format;
    private String parameters;

    // Additional computed fields
    private String downloadUrl;
    private Long fileSize;
}
