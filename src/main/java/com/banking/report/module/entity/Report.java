package com.banking.report.module.entity;

import com.banking.core.enums.ReportType;
import com.banking.core.enums.ReportFormat;
import com.banking.core.auth.module.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Report Entity
 */
@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_type", columnList = "report_type"),
        @Index(name = "idx_generated_by", columnList = "generated_by"),
        @Index(name = "idx_generated_date", columnList = "generated_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 50)
    private ReportType reportType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(name = "generated_date", nullable = false)
    private LocalDateTime generatedDate;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportFormat format;

    @Column(columnDefinition = "TEXT")
    private String parameters; // JSON string

    @PrePersist
    protected void onCreate() {
        if (generatedDate == null) {
            generatedDate = LocalDateTime.now();
        }
    }
}
