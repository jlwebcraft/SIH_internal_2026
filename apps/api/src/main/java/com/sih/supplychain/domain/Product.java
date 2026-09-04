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
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 120)
    private String category;

    @Column(name = "unit_cost", precision = 19, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "selling_price", precision = 19, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "production_time_hours", precision = 10, scale = 2)
    private BigDecimal productionTimeHours;

    @Column(length = 50)
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<ProductMaterial> productMaterials = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<ProductionOrder> productionOrders = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<CustomerOrderItem> customerOrderItems = new LinkedHashSet<>();

    protected Product() {
    }

    public Long getId() {
        return id;
    }
}
