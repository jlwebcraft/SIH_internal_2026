package com.sih.supplychain.service;

import com.sih.supplychain.domain.Supplier;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.PurchaseOrderRepository;
import com.sih.supplychain.repository.SupplierMaterialRepository;
import com.sih.supplychain.repository.SupplierPerformanceRepository;
import com.sih.supplychain.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTests {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierMaterialRepository supplierMaterialRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierPerformanceRepository supplierPerformanceRepository;

    @InjectMocks
    private SupplierService supplierService;

    @Test
    void createSupplierSavesValidSupplier() {
        Supplier supplier = new Supplier("Acme Components", "SUP-001");
        supplier.setLeadTimeDays(5);
        supplier.setCapacity(new BigDecimal("1000.000"));
        supplier.setReliabilityScore(new BigDecimal("95.00"));

        when(this.supplierRepository.existsByCode("SUP-001")).thenReturn(false);
        when(this.supplierRepository.save(supplier)).thenReturn(supplier);

        Supplier created = this.supplierService.createSupplier(supplier);

        assertThat(created).isSameAs(supplier);
        verify(this.supplierRepository).save(supplier);
    }

    @Test
    void createSupplierRejectsDuplicateCode() {
        Supplier supplier = new Supplier("Acme Components", "SUP-001");
        when(this.supplierRepository.existsByCode("SUP-001")).thenReturn(true);

        assertThatThrownBy(() -> this.supplierService.createSupplier(supplier))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void missingSupplierLookupIsRejected() {
        when(this.supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.supplierService.getSupplierById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createSupplierRejectsInvalidNumericalValues() {
        Supplier supplier = new Supplier("Acme Components", "SUP-001");
        supplier.setLeadTimeDays(-1);

        assertThatThrownBy(() -> this.supplierService.createSupplier(supplier))
                .isInstanceOf(InvalidBusinessStateException.class);
    }
}
