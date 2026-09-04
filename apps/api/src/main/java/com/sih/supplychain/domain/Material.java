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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "materials")
public class Material {

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

    @Column(length = 50)
    private String unit;

    @Column(name = "unit_cost", precision = 19, scale = 2)
    private BigDecimal unitCost;

    @Column(length = 50)
    private String criticality;

    @Column(name = "current_stock", precision = 19, scale = 3)
    private BigDecimal currentStock;

    @Column(name = "safety_stock", precision = 19, scale = 3)
    private BigDecimal safetyStock;

    @Column(name = "reorder_point", precision = 19, scale = 3)
    private BigDecimal reorderPoint;

    @Column(name = "daily_consumption", precision = 19, scale = 3)
    private BigDecimal dailyConsumption;

    @Column(length = 50)
    private String status;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;

    @OneToMany(mappedBy = "material", fetch = FetchType.LAZY)
    private Set<SupplierMaterial> supplierMaterials = new LinkedHashSet<>();

    @OneToMany(mappedBy = "material", fetch = FetchType.LAZY)
    private Set<ProductMaterial> productMaterials = new LinkedHashSet<>();

    @OneToMany(mappedBy = "material", fetch = FetchType.LAZY)
    private Set<Inventory> inventories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "material", fetch = FetchType.LAZY)
    private Set<PurchaseOrderItem> purchaseOrderItems = new LinkedHashSet<>();

    protected Material() {
    }

    public Material(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCriticality() {
        return criticality;
    }

    public void setCriticality(String criticality) {
        this.criticality = criticality;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
