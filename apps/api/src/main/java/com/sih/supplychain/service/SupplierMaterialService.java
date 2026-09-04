package com.sih.supplychain.service;

import com.sih.supplychain.domain.Material;
import com.sih.supplychain.domain.Supplier;
import com.sih.supplychain.domain.SupplierMaterial;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.MaterialRepository;
import com.sih.supplychain.repository.SupplierMaterialRepository;
import com.sih.supplychain.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SupplierMaterialService {

    private final SupplierMaterialRepository supplierMaterialRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;

    public SupplierMaterialService(
            SupplierMaterialRepository supplierMaterialRepository,
            SupplierRepository supplierRepository,
            MaterialRepository materialRepository
    ) {
        this.supplierMaterialRepository = supplierMaterialRepository;
        this.supplierRepository = supplierRepository;
        this.materialRepository = materialRepository;
    }

    @Transactional
    public SupplierMaterial createSupplierMaterial(Long supplierId, Long materialId, SupplierMaterial details) {
        Supplier supplier = getSupplier(supplierId);
        Material material = getMaterial(materialId);
        if (this.supplierMaterialRepository.existsBySupplierIdAndMaterialId(supplierId, materialId)) {
            throw new DuplicateResourceException("Supplier material relationship already exists");
        }
        validateDetails(details);

        SupplierMaterial supplierMaterial = new SupplierMaterial(supplier, material);
        applyMutableFields(supplierMaterial, details);
        return this.supplierMaterialRepository.save(supplierMaterial);
    }

    public SupplierMaterial getSupplierMaterialById(Long id) {
        return this.supplierMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier material not found with id: " + id));
    }

    public List<SupplierMaterial> listMaterialsSuppliedBySupplier(Long supplierId) {
        getSupplier(supplierId);
        return this.supplierMaterialRepository.findBySupplierId(supplierId);
    }

    public List<SupplierMaterial> listSuppliersForMaterial(Long materialId) {
        getMaterial(materialId);
        return this.supplierMaterialRepository.findByMaterialId(materialId);
    }

    @Transactional
    public SupplierMaterial updateSupplierMaterial(Long id, SupplierMaterial changes) {
        SupplierMaterial supplierMaterial = getSupplierMaterialById(id);
        validateDetails(changes);
        applyMutableFields(supplierMaterial, changes);
        return this.supplierMaterialRepository.save(supplierMaterial);
    }

    @Transactional
    public void removeSupplierMaterial(Long id) {
        SupplierMaterial supplierMaterial = getSupplierMaterialById(id);
        this.supplierMaterialRepository.delete(supplierMaterial);
    }

    private Supplier getSupplier(Long supplierId) {
        return this.supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
    }

    private Material getMaterial(Long materialId) {
        return this.materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + materialId));
    }

    private void validateDetails(SupplierMaterial details) {
        if (details == null) {
            throw new InvalidBusinessStateException("Supplier material details are required");
        }
        BusinessValidation.requireNonNegative(details.getUnitPrice(), "Supplier material unit price");
        BusinessValidation.requireNonNegative(details.getLeadTimeDays(), "Supplier material lead time");
        BusinessValidation.requireNonNegative(details.getMinimumOrderQuantity(), "Supplier material minimum order quantity");
        BusinessValidation.requireNonNegative(details.getMaximumCapacity(), "Supplier material maximum capacity");
        BusinessValidation.requirePercentageRange(details.getReliabilityScore(), "Supplier material reliability score");
    }

    private void applyMutableFields(SupplierMaterial target, SupplierMaterial source) {
        target.setUnitPrice(source.getUnitPrice());
        target.setLeadTimeDays(source.getLeadTimeDays());
        target.setMinimumOrderQuantity(source.getMinimumOrderQuantity());
        target.setMaximumCapacity(source.getMaximumCapacity());
        target.setReliabilityScore(source.getReliabilityScore());
        target.setStatus(source.getStatus());
    }
}
