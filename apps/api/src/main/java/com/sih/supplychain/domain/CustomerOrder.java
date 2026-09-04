package com.sih.supplychain.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "order_number", nullable = false, unique = true, length = 80)
    private String orderNumber;

    @NotBlank
    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "required_delivery_date")
    private LocalDate requiredDeliveryDate;

    @Column(length = 50)
    private String status;

    @Column(length = 50)
    private String priority;

    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "customerOrder", fetch = FetchType.LAZY)
    private Set<CustomerOrderItem> items = new LinkedHashSet<>();

    protected CustomerOrder() {
    }

    public Long getId() {
        return id;
    }
}
