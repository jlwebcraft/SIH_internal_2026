package com.sih.supplychain.service;

import com.sih.supplychain.domain.Inventory;
import com.sih.supplychain.domain.Material;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.repository.InventoryRepository;
import com.sih.supplychain.repository.MaterialRepository;
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
class InventoryServiceTests {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void createInventorySavesValidInventory() {
        Material material = new Material("MAT-001", "Steel Sheet");
        Inventory details = new Inventory(material, "MAIN");
        details.setQuantityOnHand(new BigDecimal("100.000"));

        when(this.materialRepository.findById(1L)).thenReturn(Optional.of(material));
        when(this.inventoryRepository.existsByMaterialIdAndWarehouseLocation(1L, "MAIN")).thenReturn(false);
        when(this.inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Inventory created = this.inventoryService.createInventory(1L, details);

        assertThat(created.getMaterial()).isSameAs(material);
        assertThat(created.getWarehouseLocation()).isEqualTo("MAIN");
        assertThat(created.getQuantityOnHand()).isEqualByComparingTo("100.000");
        verify(this.inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void createInventoryRejectsDuplicateMaterialWarehouse() {
        Material material = new Material("MAT-001", "Steel Sheet");
        Inventory details = new Inventory(material, "MAIN");

        when(this.materialRepository.findById(1L)).thenReturn(Optional.of(material));
        when(this.inventoryRepository.existsByMaterialIdAndWarehouseLocation(1L, "MAIN")).thenReturn(true);

        assertThatThrownBy(() -> this.inventoryService.createInventory(1L, details))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createInventoryRejectsNegativeStock() {
        Material material = new Material("MAT-001", "Steel Sheet");
        Inventory details = new Inventory(material, "MAIN");
        details.setQuantityOnHand(new BigDecimal("-1.000"));

        when(this.materialRepository.findById(1L)).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> this.inventoryService.createInventory(1L, details))
                .isInstanceOf(InvalidBusinessStateException.class);
    }

    @Test
    void adjustStockAddsAdjustmentToCurrentStock() {
        Inventory inventory = new Inventory(new Material("MAT-001", "Steel Sheet"), "MAIN");
        inventory.setQuantityOnHand(new BigDecimal("100.000"));

        when(this.inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(this.inventoryRepository.save(inventory)).thenReturn(inventory);

        Inventory adjusted = this.inventoryService.adjustStock(1L, new BigDecimal("-20.000"));

        assertThat(adjusted.getQuantityOnHand()).isEqualByComparingTo("80.000");
    }

    @Test
    void adjustStockRejectsNegativeResult() {
        Inventory inventory = new Inventory(new Material("MAT-001", "Steel Sheet"), "MAIN");
        inventory.setQuantityOnHand(new BigDecimal("10.000"));

        when(this.inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> this.inventoryService.adjustStock(1L, new BigDecimal("-20.000")))
                .isInstanceOf(InvalidBusinessStateException.class);
    }
}
