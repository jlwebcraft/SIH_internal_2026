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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventories",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventories_material_warehouse",
                columnNames = {"material_id", "warehouse_location"}))
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @NotBlank
    @Column(name = "warehouse_location", nullable = false, length = 200)
    private String warehouseLocation;

    @Column(name = "quantity_on_hand", precision = 19, scale = 3)
    private BigDecimal quantityOnHand;

    @Column(name = "quantity_reserved", precision = 19, scale = 3)
    private BigDecimal quantityReserved;

    @Column(name = "quantity_incoming", precision = 19, scale = 3)
    private BigDecimal quantityIncoming;

    @Column(name = "safety_stock", precision = 19, scale = 3)
    private BigDecimal safetyStock;

    @Column(name = "reorder_point", precision = 19, scale = 3)
    private BigDecimal reorderPoint;

    @Column(name = "last_updated")
    @UpdateTimestamp
    private Instant lastUpdated;

    protected Inventory() {
    }

    public Inventory(Material material, String warehouseLocation) {
        this.material = material;
        this.warehouseLocation = warehouseLocation;
    }

    public Long getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getWarehouseLocation() {
        return warehouseLocation;
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(BigDecimal quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public BigDecimal getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(BigDecimal quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public BigDecimal getQuantityIncoming() {
        return quantityIncoming;
    }

    public void setQuantityIncoming(BigDecimal quantityIncoming) {
        this.quantityIncoming = quantityIncoming;
    }

    public BigDecimal getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(BigDecimal safetyStock) {
        this.safetyStock = safetyStock;
    }

    public BigDecimal getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(BigDecimal reorderPoint) {
        this.reorderPoint = reorderPoint;
    }
}
