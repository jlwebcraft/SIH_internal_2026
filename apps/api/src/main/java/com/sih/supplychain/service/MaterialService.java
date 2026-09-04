package com.sih.supplychain.service;

import com.sih.supplychain.domain.Material;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.InventoryRepository;
import com.sih.supplychain.repository.MaterialRepository;
import com.sih.supplychain.repository.ProductMaterialRepository;
import com.sih.supplychain.repository.PurchaseOrderItemRepository;
import com.sih.supplychain.repository.SupplierMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final SupplierMaterialRepository supplierMaterialRepository;
    private final ProductMaterialRepository productMaterialRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    public MaterialService(
            MaterialRepository materialRepository,
            SupplierMaterialRepository supplierMaterialRepository,
            ProductMaterialRepository productMaterialRepository,
            InventoryRepository inventoryRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository
    ) {
        this.materialRepository = materialRepository;
        this.supplierMaterialRepository = supplierMaterialRepository;
        this.productMaterialRepository = productMaterialRepository;
        this.inventoryRepository = inventoryRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
    }

    @Transactional
    public Material createMaterial(Material material) {
        validateMaterial(material);
        if (this.materialRepository.existsByCode(material.getCode())) {
            throw new DuplicateResourceException("Material code already exists: " + material.getCode());
        }
        return this.materialRepository.save(material);
    }

    public Material getMaterialById(Long id) {
        return this.materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + id));
    }

    public Material getMaterialByCode(String code) {
        BusinessValidation.requireText(code, "Material code");
        return this.materialRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with code: " + code));
    }

    public List<Material> getAllMaterials() {
        return this.materialRepository.findAll();
    }

    @Transactional
    public Material updateMaterial(Long id, Material changes) {
        Material material = getMaterialById(id);
        validateMaterial(changes);
        if (!Objects.equals(material.getCode(), changes.getCode())
                && this.materialRepository.existsByCode(changes.getCode())) {
            throw new DuplicateResourceException("Material code already exists: " + changes.getCode());
        }

        material.setCode(changes.getCode());
        material.setName(changes.getName());
        material.setDescription(changes.getDescription());
        material.setCategory(changes.getCategory());
        material.setUnit(changes.getUnit());
        material.setUnitCost(changes.getUnitCost());
        material.setCriticality(changes.getCriticality());
        material.setCurrentStock(changes.getCurrentStock());
        material.setSafetyStock(changes.getSafetyStock());
        material.setReorderPoint(changes.getReorderPoint());
        material.setDailyConsumption(changes.getDailyConsumption());
        material.setStatus(changes.getStatus());
        return this.materialRepository.save(material);
    }

    @Transactional
    public void deleteMaterial(Long id) {
        Material material = getMaterialById(id);
        if (this.supplierMaterialRepository.existsByMaterialId(id)
                || this.productMaterialRepository.existsByMaterialId(id)
                || this.inventoryRepository.existsByMaterialId(id)
                || this.purchaseOrderItemRepository.existsByMaterialId(id)) {
            throw new InvalidBusinessStateException("Material has dependent operational records and cannot be deleted");
        }
        this.materialRepository.delete(material);
    }

    @Transactional
    public Material deactivateMaterial(Long id) {
        Material material = getMaterialById(id);
        material.setStatus("INACTIVE");
        return this.materialRepository.save(material);
    }

    private void validateMaterial(Material material) {
        if (material == null) {
            throw new InvalidBusinessStateException("Material is required");
        }
        BusinessValidation.requireText(material.getCode(), "Material code");
        BusinessValidation.requireText(material.getName(), "Material name");
        BusinessValidation.requireNonNegative(material.getUnitCost(), "Material unit cost");
        BusinessValidation.requireNonNegative(material.getCurrentStock(), "Material current stock");
        BusinessValidation.requireNonNegative(material.getSafetyStock(), "Material safety stock");
        BusinessValidation.requireNonNegative(material.getReorderPoint(), "Material reorder point");
        BusinessValidation.requireNonNegative(material.getDailyConsumption(), "Material daily consumption");
    }
}
