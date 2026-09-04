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

@Entity
@Table(name = "product_materials",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_materials_product_material",
                columnNames = {"product_id", "material_id"}))
public class ProductMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @NotNull
    @Column(name = "quantity_required", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityRequired;

    @Column(length = 50)
    private String unit;

    @Column(name = "wastage_percentage", precision = 5, scale = 2)
    private BigDecimal wastagePercentage;

    protected ProductMaterial() {
    }

    public Long getId() {
        return id;
    }
}
