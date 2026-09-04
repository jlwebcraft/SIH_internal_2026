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
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "tracking_number", length = 120)
    private String trackingNumber;

    @Column(name = "dispatch_date")
    private LocalDate dispatchDate;

    @Column(name = "expected_arrival_date")
    private LocalDate expectedArrivalDate;

    @Column(name = "actual_arrival_date")
    private LocalDate actualArrivalDate;

    @Column(length = 50)
    private String status;

    @Column(name = "delay_days")
    private Integer delayDays;

    @Column(length = 1000)
    private String notes;

    protected Delivery() {
    }

    public Delivery(PurchaseOrder purchaseOrder, String trackingNumber) {
        this.purchaseOrder = purchaseOrder;
        this.trackingNumber = trackingNumber;
    }

    public Long getId() {
        return id;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }
}
