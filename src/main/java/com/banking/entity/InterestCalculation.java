package com.banking.entity;

import com.banking.enums.CalculationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Interest Calculation Entity
 * 
 * Stores records of all interest calculations performed on accounts.
 * This entity is used for audit trails and reporting purposes.
 * 
 * @author Banking System
 */
@Entity
@Table(name = "interest_calculations", indexes = {
        @Index(name = "idx_account_id", columnList = "account_id"),
        @Index(name = "idx_calculation_date", columnList = "calculation_date"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterestCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "interest_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    @Column(name = "strategy_used", nullable = false, length = 100)
    private String strategyUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CalculationStatus status = CalculationStatus.SUCCESS;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "previous_balance", precision = 19, scale = 2)
    private BigDecimal previousBalance;

    @Column(name = "new_balance", precision = 19, scale = 2)
    private BigDecimal newBalance;

    @Column(name = "interest_rate", precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "calculation_duration_ms")
    private Long calculationDurationMs;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (calculationDate == null) {
            calculationDate = LocalDate.now();
        }
    }
}
