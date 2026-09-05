package com.sih.supplychain.service;

import com.sih.supplychain.domain.Delivery;
import com.sih.supplychain.domain.PurchaseOrder;
import com.sih.supplychain.domain.Supplier;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.repository.DeliveryRepository;
import com.sih.supplychain.repository.PurchaseOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTests {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @InjectMocks
    private DeliveryService deliveryService;

    @Test
    void createDeliveryDefaultsToPending() {
        PurchaseOrder purchaseOrder = new PurchaseOrder("PO-1001", new Supplier("Supplier ABC", "SUP-001"));
        Delivery details = new Delivery(purchaseOrder, "TRK-001");
        details.setDispatchDate(LocalDate.of(2026, 9, 5));
        details.setExpectedArrivalDate(LocalDate.of(2026, 9, 8));

        when(this.purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(this.deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Delivery created = this.deliveryService.createDelivery(1L, details);

        assertThat(created.getPurchaseOrder()).isSameAs(purchaseOrder);
        assertThat(created.getStatus()).isEqualTo(DeliveryService.STATUS_PENDING);
        assertThat(created.getTrackingNumber()).isEqualTo("TRK-001");
    }

    @Test
    void createDeliveryRejectsInvalidArrivalDate() {
        PurchaseOrder purchaseOrder = new PurchaseOrder("PO-1001", new Supplier("Supplier ABC", "SUP-001"));
        Delivery details = new Delivery(purchaseOrder, "TRK-001");
        details.setDispatchDate(LocalDate.of(2026, 9, 8));
        details.setActualArrivalDate(LocalDate.of(2026, 9, 7));

        when(this.purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));

        assertThatThrownBy(() -> this.deliveryService.createDelivery(1L, details))
                .isInstanceOf(InvalidBusinessStateException.class);
    }

    @Test
    void deliveryCanMoveThroughValidTransitLifecycle() {
        Delivery delivery = new Delivery(new PurchaseOrder("PO-1001", new Supplier("Supplier ABC", "SUP-001")), "TRK-001");
        delivery.setStatus(DeliveryService.STATUS_PENDING);

        when(this.deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(this.deliveryRepository.save(delivery)).thenReturn(delivery);

        assertThat(this.deliveryService.dispatchDelivery(1L).getStatus()).isEqualTo(DeliveryService.STATUS_DISPATCHED);
        assertThat(this.deliveryService.markInTransit(1L).getStatus()).isEqualTo(DeliveryService.STATUS_IN_TRANSIT);
        assertThat(this.deliveryService.markDelivered(1L).getStatus()).isEqualTo(DeliveryService.STATUS_DELIVERED);
    }

    @Test
    void deliveredDeliveryCannotBeCancelled() {
        Delivery delivery = new Delivery(new PurchaseOrder("PO-1001", new Supplier("Supplier ABC", "SUP-001")), "TRK-001");
        delivery.setStatus(DeliveryService.STATUS_DELIVERED);

        when(this.deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> this.deliveryService.cancelDelivery(1L))
                .isInstanceOf(InvalidBusinessStateException.class);
    }
}
