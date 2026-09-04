package com.sih.supplychain.service;

import com.sih.supplychain.domain.Material;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.repository.InventoryRepository;
import com.sih.supplychain.repository.MaterialRepository;
import com.sih.supplychain.repository.ProductMaterialRepository;
import com.sih.supplychain.repository.PurchaseOrderItemRepository;
import com.sih.supplychain.repository.SupplierMaterialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTests {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private SupplierMaterialRepository supplierMaterialRepository;

    @Mock
    private ProductMaterialRepository productMaterialRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @InjectMocks
    private MaterialService materialService;

    @Test
    void createMaterialSavesValidMaterial() {
        Material material = new Material("MAT-001", "Steel Sheet");
        material.setUnitCost(new BigDecimal("12.50"));
        material.setSafetyStock(new BigDecimal("10.000"));

        when(this.materialRepository.existsByCode("MAT-001")).thenReturn(false);
        when(this.materialRepository.save(material)).thenReturn(material);

        Material created = this.materialService.createMaterial(material);

        assertThat(created).isSameAs(material);
        verify(this.materialRepository).save(material);
    }

    @Test
    void createMaterialRejectsDuplicateCode() {
        Material material = new Material("MAT-001", "Steel Sheet");
        when(this.materialRepository.existsByCode("MAT-001")).thenReturn(true);

        assertThatThrownBy(() -> this.materialService.createMaterial(material))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createMaterialRejectsNegativeQuantityOrCost() {
        Material material = new Material("MAT-001", "Steel Sheet");
        material.setUnitCost(new BigDecimal("-1.00"));

        assertThatThrownBy(() -> this.materialService.createMaterial(material))
                .isInstanceOf(InvalidBusinessStateException.class);
    }
}
