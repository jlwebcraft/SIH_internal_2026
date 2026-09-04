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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierMaterialServiceTests {

    @Mock
    private SupplierMaterialRepository supplierMaterialRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private SupplierMaterialService supplierMaterialService;

    @Test
    void createSupplierMaterialSavesValidRelationship() {
        Supplier supplier = new Supplier("Acme Components", "SUP-001");
        Material material = new Material("MAT-001", "Steel Sheet");
        SupplierMaterial details = new SupplierMaterial(supplier, material);
        details.setUnitPrice(new BigDecimal("10.00"));

        when(this.supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(this.materialRepository.findById(2L)).thenReturn(Optional.of(material));
        when(this.supplierMaterialRepository.existsBySupplierIdAndMaterialId(1L, 2L)).thenReturn(false);
        when(this.supplierMaterialRepository.save(any(SupplierMaterial.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SupplierMaterial created = this.supplierMaterialService.createSupplierMaterial(1L, 2L, details);

        assertThat(created.getSupplier()).isSameAs(supplier);
        assertThat(created.getMaterial()).isSameAs(material);
        assertThat(created.getUnitPrice()).isEqualByComparingTo("10.00");
        verify(this.supplierMaterialRepository).save(any(SupplierMaterial.class));
    }

    @Test
    void createSupplierMaterialRejectsMissingSupplier() {
        when(this.supplierRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.supplierMaterialService.createSupplierMaterial(1L, 2L, validDetails()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createSupplierMaterialRejectsMissingMaterial() {
        Supplier supplier = new Supplier("Acme Components", "SUP-001");
        when(this.supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(this.materialRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.supplierMaterialService.createSupplierMaterial(1L, 2L, validDetails()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createSupplierMaterialRejectsDuplicateRelationship() {
        Supplier supplier = new Supplier("Acme Components", "SUP-001");
        Material material = new Material("MAT-001", "Steel Sheet");
        when(this.supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(this.materialRepository.findById(2L)).thenReturn(Optional.of(material));
        when(this.supplierMaterialRepository.existsBySupplierIdAndMaterialId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> this.supplierMaterialService.createSupplierMaterial(1L, 2L, validDetails()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createSupplierMaterialRejectsInvalidPriceOrCapacity() {
        Supplier supplier = new Supplier("Acme Components", "SUP-001");
        Material material = new Material("MAT-001", "Steel Sheet");
        SupplierMaterial details = validDetails();
        details.setMaximumCapacity(new BigDecimal("-1.000"));

        when(this.supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(this.materialRepository.findById(2L)).thenReturn(Optional.of(material));
        when(this.supplierMaterialRepository.existsBySupplierIdAndMaterialId(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> this.supplierMaterialService.createSupplierMaterial(1L, 2L, details))
                .isInstanceOf(InvalidBusinessStateException.class);
    }

    private SupplierMaterial validDetails() {
        SupplierMaterial details = new SupplierMaterial(
                new Supplier("Acme Components", "SUP-001"),
                new Material("MAT-001", "Steel Sheet")
        );
        details.setUnitPrice(new BigDecimal("10.00"));
        return details;
    }
}
