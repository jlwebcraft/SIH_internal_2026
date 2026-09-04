package com.sih.supplychain.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String name;

    @NotBlank
    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Email
    @Column(length = 320)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 120)
    private String city;

    @Column(length = 120)
    private String state;

    @Column(length = 120)
    private String country;

    @PositiveOrZero
    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(precision = 19, scale = 3)
    private BigDecimal capacity;

    @Column(name = "reliability_score", precision = 5, scale = 2)
    private BigDecimal reliabilityScore;

    @Column(length = 50)
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private Set<SupplierMaterial> supplierMaterials = new LinkedHashSet<>();

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private Set<PurchaseOrder> purchaseOrders = new LinkedHashSet<>();

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private Set<SupplierPerformance> performanceEntries = new LinkedHashSet<>();

    protected Supplier() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
