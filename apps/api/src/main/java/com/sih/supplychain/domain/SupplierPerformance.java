package com.sih.supplychain.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "supplier_performances",
        uniqueConstraints = @UniqueConstraint(name = "uk_supplier_performances_supplier_date",
                columnNames = {"supplier_id", "evaluation_date"}))
public class SupplierPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "evaluation_date")
    private LocalDate evaluationDate;

    @Column(name = "on_time_delivery_rate", precision = 5, scale = 2)
    private BigDecimal onTimeDeliveryRate;

    @Column(name = "average_delay_days", precision = 10, scale = 2)
    private BigDecimal averageDelayDays;

    @Column(name = "lead_time_variance", precision = 10, scale = 2)
    private BigDecimal leadTimeVariance;

    @Column(name = "fulfillment_rate", precision = 5, scale = 2)
    private BigDecimal fulfillmentRate;

    @Column(name = "rejection_rate", precision = 5, scale = 2)
    private BigDecimal rejectionRate;

    @Column(name = "capacity_utilization", precision = 5, scale = 2)
    private BigDecimal capacityUtilization;

    @Column(name = "disruption_count")
    private Integer disruptionCount;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    protected SupplierPerformance() {
    }

    public SupplierPerformance(Supplier supplier, LocalDate evaluationDate) {
        this.supplier = supplier;
        this.evaluationDate = evaluationDate;
    }

    public Long getId() {
        return id;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public LocalDate getEvaluationDate() {
        return evaluationDate;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }
}
