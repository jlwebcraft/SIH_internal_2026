package com.sih.supplychain.service;

import com.sih.supplychain.domain.Material;
import com.sih.supplychain.domain.PurchaseOrder;
import com.sih.supplychain.domain.PurchaseOrderItem;
import com.sih.supplychain.domain.Supplier;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.repository.MaterialRepository;
import com.sih.supplychain.repository.PurchaseOrderItemRepository;
import com.sih.supplychain.repository.PurchaseOrderRepository;
import com.sih.supplychain.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTests {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    @Test
    void createPurchaseOrderCalculatesTotalAndPersistsItemsAtomically() {
        Supplier supplier = new Supplier("Supplier ABC", "SUP-001");
        Material steel = new Material("MAT-ST", "Steel");
        Material copper = new Material("MAT-CU", "Copper");
        LocalDate orderDate = LocalDate.of(2026, 9, 4);
        PurchaseOrderService.CreatePurchaseOrderCommand command = new PurchaseOrderService.CreatePurchaseOrderCommand(
                "PO-1001",
                1L,
                orderDate,
                LocalDate.of(2026, 9, 12),
                List.of(
                        item(10L, "500.000", "420.50", LocalDate.of(2026, 9, 10)),
                        item(11L, "2.000", "10.25", LocalDate.of(2026, 9, 11))
                )
        );

        when(this.supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(this.purchaseOrderRepository.existsByPoNumber("PO-1001")).thenReturn(false);
        when(this.materialRepository.findById(10L)).thenReturn(Optional.of(steel));
        when(this.materialRepository.findById(11L)).thenReturn(Optional.of(copper));
        when(this.purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(this.purchaseOrderItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrder created = this.purchaseOrderService.createPurchaseOrder(command);

        assertThat(created.getStatus()).isEqualTo(PurchaseOrderService.STATUS_DRAFT);
        assertThat(created.getTotalAmount()).isEqualByComparingTo("210270.50");
        assertThat(created.getItems()).hasSize(2);
        assertThat(created.getItems())
                .extracting(PurchaseOrderItem::getStatus)
                .containsOnly("OPEN");
        verify(this.purchaseOrderRepository).save(any(PurchaseOrder.class));
        verify(this.purchaseOrderItemRepository).saveAll(anyList());
    }

    @Test
    void createPurchaseOrderRejectsDuplicatePoNumber() {
        PurchaseOrderService.CreatePurchaseOrderCommand command = new PurchaseOrderService.CreatePurchaseOrderCommand(
                "PO-1001",
                1L,
                LocalDate.of(2026, 9, 4),
                LocalDate.of(2026, 9, 12),
                List.of(item(10L, "1.000", "10.00", LocalDate.of(2026, 9, 10)))
        );

        when(this.supplierRepository.findById(1L)).thenReturn(Optional.of(new Supplier("Supplier ABC", "SUP-001")));
        when(this.purchaseOrderRepository.existsByPoNumber("PO-1001")).thenReturn(true);

        assertThatThrownBy(() -> this.purchaseOrderService.createPurchaseOrder(command))
                .isInstanceOf(DuplicateResourceException.class);
        verify(this.purchaseOrderItemRepository, never()).saveAll(anyList());
    }

    @Test
    void createPurchaseOrderRejectsInvalidItemBeforeSavingOrder() {
        PurchaseOrderService.CreatePurchaseOrderCommand command = new PurchaseOrderService.CreatePurchaseOrderCommand(
                "PO-1002",
                1L,
                LocalDate.of(2026, 9, 4),
                LocalDate.of(2026, 9, 12),
                List.of(item(10L, "1.000", "10.00", LocalDate.of(2026, 9, 10)), item(11L, "0", "5.00", null))
        );

        when(this.supplierRepository.findById(1L)).thenReturn(Optional.of(new Supplier("Supplier ABC", "SUP-001")));
        when(this.purchaseOrderRepository.existsByPoNumber("PO-1002")).thenReturn(false);
        when(this.materialRepository.findById(10L)).thenReturn(Optional.of(new Material("MAT-ST", "Steel")));

        assertThatThrownBy(() -> this.purchaseOrderService.createPurchaseOrder(command))
                .isInstanceOf(InvalidBusinessStateException.class);
        verify(this.purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void placePurchaseOrderAllowsOnlyDraftOrders() {
        PurchaseOrder purchaseOrder = new PurchaseOrder("PO-1001", new Supplier("Supplier ABC", "SUP-001"));
        purchaseOrder.setStatus(PurchaseOrderService.STATUS_DRAFT);

        when(this.purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(this.purchaseOrderRepository.save(purchaseOrder)).thenReturn(purchaseOrder);

        PurchaseOrder placed = this.purchaseOrderService.placePurchaseOrder(1L);

        assertThat(placed.getStatus()).isEqualTo(PurchaseOrderService.STATUS_PLACED);
    }

    @Test
    void placePurchaseOrderRejectsInvalidTransition() {
        PurchaseOrder purchaseOrder = new PurchaseOrder("PO-1001", new Supplier("Supplier ABC", "SUP-001"));
        purchaseOrder.setStatus(PurchaseOrderService.STATUS_PLACED);

        when(this.purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));

        assertThatThrownBy(() -> this.purchaseOrderService.placePurchaseOrder(1L))
                .isInstanceOf(InvalidBusinessStateException.class);
    }

    private PurchaseOrderService.PurchaseOrderItemCommand item(
            Long materialId,
            String quantity,
            String unitPrice,
            LocalDate expectedDate
    ) {
        return new PurchaseOrderService.PurchaseOrderItemCommand(
                materialId,
                new BigDecimal(quantity),
                new BigDecimal(unitPrice),
                expectedDate
        );
    }
}
