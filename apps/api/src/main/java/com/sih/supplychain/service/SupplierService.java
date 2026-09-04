package com.sih.supplychain.service;

import com.sih.supplychain.domain.Supplier;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.PurchaseOrderRepository;
import com.sih.supplychain.repository.SupplierMaterialRepository;
import com.sih.supplychain.repository.SupplierPerformanceRepository;
import com.sih.supplychain.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMaterialRepository supplierMaterialRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierPerformanceRepository supplierPerformanceRepository;

    public SupplierService(
            SupplierRepository supplierRepository,
            SupplierMaterialRepository supplierMaterialRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            SupplierPerformanceRepository supplierPerformanceRepository
    ) {
        this.supplierRepository = supplierRepository;
        this.supplierMaterialRepository = supplierMaterialRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierPerformanceRepository = supplierPerformanceRepository;
    }

    @Transactional
    public Supplier createSupplier(Supplier supplier) {
        validateSupplier(supplier);
        if (this.supplierRepository.existsByCode(supplier.getCode())) {
            throw new DuplicateResourceException("Supplier code already exists: " + supplier.getCode());
        }
        return this.supplierRepository.save(supplier);
    }

    public Supplier getSupplierById(Long id) {
        return this.supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
    }

    public Supplier getSupplierByCode(String code) {
        BusinessValidation.requireText(code, "Supplier code");
        return this.supplierRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with code: " + code));
    }

    public List<Supplier> getAllSuppliers() {
        return this.supplierRepository.findAll();
    }

    @Transactional
    public Supplier updateSupplier(Long id, Supplier changes) {
        Supplier supplier = getSupplierById(id);
        validateSupplier(changes);
        if (!Objects.equals(supplier.getCode(), changes.getCode())
                && this.supplierRepository.existsByCode(changes.getCode())) {
            throw new DuplicateResourceException("Supplier code already exists: " + changes.getCode());
        }

        supplier.setName(changes.getName());
        supplier.setCode(changes.getCode());
        supplier.setContactPerson(changes.getContactPerson());
        supplier.setEmail(changes.getEmail());
        supplier.setPhone(changes.getPhone());
        supplier.setAddress(changes.getAddress());
        supplier.setCity(changes.getCity());
        supplier.setState(changes.getState());
        supplier.setCountry(changes.getCountry());
        supplier.setLeadTimeDays(changes.getLeadTimeDays());
        supplier.setCapacity(changes.getCapacity());
        supplier.setReliabilityScore(changes.getReliabilityScore());
        supplier.setStatus(changes.getStatus());
        return this.supplierRepository.save(supplier);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = getSupplierById(id);
        if (this.supplierMaterialRepository.existsBySupplierId(id)
                || this.purchaseOrderRepository.existsBySupplierId(id)
                || this.supplierPerformanceRepository.existsBySupplierId(id)) {
            throw new InvalidBusinessStateException("Supplier has dependent operational records and cannot be deleted");
        }
        this.supplierRepository.delete(supplier);
    }

    @Transactional
    public Supplier deactivateSupplier(Long id) {
        Supplier supplier = getSupplierById(id);
        supplier.setStatus("INACTIVE");
        return this.supplierRepository.save(supplier);
    }

    private void validateSupplier(Supplier supplier) {
        if (supplier == null) {
            throw new InvalidBusinessStateException("Supplier is required");
        }
        BusinessValidation.requireText(supplier.getName(), "Supplier name");
        BusinessValidation.requireText(supplier.getCode(), "Supplier code");
        BusinessValidation.requireNonNegative(supplier.getLeadTimeDays(), "Supplier lead time");
        BusinessValidation.requireNonNegative(supplier.getCapacity(), "Supplier capacity");
        BusinessValidation.requirePercentageRange(supplier.getReliabilityScore(), "Supplier reliability score");
    }
}
