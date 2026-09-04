package com.sih.supplychain.service;

import com.sih.supplychain.domain.Inventory;
import com.sih.supplychain.domain.Material;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.InventoryRepository;
import com.sih.supplychain.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final MaterialRepository materialRepository;

    public InventoryService(InventoryRepository inventoryRepository, MaterialRepository materialRepository) {
        this.inventoryRepository = inventoryRepository;
        this.materialRepository = materialRepository;
    }

    @Transactional
    public Inventory createInventory(Long materialId, Inventory details) {
        Material material = getMaterial(materialId);
        validateInventory(details);
        if (this.inventoryRepository.existsByMaterialIdAndWarehouseLocation(materialId, details.getWarehouseLocation())) {
            throw new DuplicateResourceException("Inventory record already exists for material and warehouse");
        }

        Inventory inventory = new Inventory(material, details.getWarehouseLocation());
        applyMutableFields(inventory, details);
        return this.inventoryRepository.save(inventory);
    }

    public Inventory getInventoryById(Long id) {
        return this.inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));
    }

    public List<Inventory> getInventoryForMaterial(Long materialId) {
        getMaterial(materialId);
        return this.inventoryRepository.findByMaterialId(materialId);
    }

    public List<Inventory> getInventoryByWarehouseLocation(String warehouseLocation) {
        BusinessValidation.requireText(warehouseLocation, "Warehouse location");
        return this.inventoryRepository.findByWarehouseLocation(warehouseLocation);
    }

    public Inventory getInventoryByMaterialAndWarehouse(Long materialId, String warehouseLocation) {
        BusinessValidation.requireText(warehouseLocation, "Warehouse location");
        return this.inventoryRepository.findByMaterialIdAndWarehouseLocation(materialId, warehouseLocation)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for material and warehouse"));
    }

    public List<Inventory> getAllInventory() {
        return this.inventoryRepository.findAll();
    }

    @Transactional
    public Inventory updateInventory(Long id, Inventory changes) {
        Inventory inventory = getInventoryById(id);
        validateInventoryQuantities(changes);
        applyMutableFields(inventory, changes);
        return this.inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory adjustStock(Long id, BigDecimal adjustment) {
        if (adjustment == null) {
            throw new InvalidBusinessStateException("Inventory adjustment is required");
        }
        Inventory inventory = getInventoryById(id);
        BigDecimal currentStock = zeroIfNull(inventory.getQuantityOnHand());
        BigDecimal adjustedStock = currentStock.add(adjustment);
        if (adjustedStock.signum() < 0) {
            throw new InvalidBusinessStateException("Inventory adjustment would make stock negative");
        }

        inventory.setQuantityOnHand(adjustedStock);
        return this.inventoryRepository.save(inventory);
    }

    private Material getMaterial(Long materialId) {
        return this.materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + materialId));
    }

    private void validateInventory(Inventory inventory) {
        if (inventory == null) {
            throw new InvalidBusinessStateException("Inventory details are required");
        }
        BusinessValidation.requireText(inventory.getWarehouseLocation(), "Warehouse location");
        BusinessValidation.requireNonNegative(inventory.getQuantityOnHand(), "Inventory quantity on hand");
        BusinessValidation.requireNonNegative(inventory.getQuantityReserved(), "Inventory quantity reserved");
        BusinessValidation.requireNonNegative(inventory.getQuantityIncoming(), "Inventory quantity incoming");
        BusinessValidation.requireNonNegative(inventory.getSafetyStock(), "Inventory safety stock");
        BusinessValidation.requireNonNegative(inventory.getReorderPoint(), "Inventory reorder point");
    }

    private void validateInventoryQuantities(Inventory inventory) {
        if (inventory == null) {
            throw new InvalidBusinessStateException("Inventory details are required");
        }
        BusinessValidation.requireNonNegative(inventory.getQuantityOnHand(), "Inventory quantity on hand");
        BusinessValidation.requireNonNegative(inventory.getQuantityReserved(), "Inventory quantity reserved");
        BusinessValidation.requireNonNegative(inventory.getQuantityIncoming(), "Inventory quantity incoming");
        BusinessValidation.requireNonNegative(inventory.getSafetyStock(), "Inventory safety stock");
        BusinessValidation.requireNonNegative(inventory.getReorderPoint(), "Inventory reorder point");
    }

    private void applyMutableFields(Inventory target, Inventory source) {
        target.setQuantityOnHand(source.getQuantityOnHand());
        target.setQuantityReserved(source.getQuantityReserved());
        target.setQuantityIncoming(source.getQuantityIncoming());
        target.setSafetyStock(source.getSafetyStock());
        target.setReorderPoint(source.getReorderPoint());
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
