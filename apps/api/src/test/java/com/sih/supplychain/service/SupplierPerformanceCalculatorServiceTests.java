package com.sih.supplychain.service;

import com.sih.supplychain.domain.Delivery;
import com.sih.supplychain.domain.Material;
import com.sih.supplychain.domain.PurchaseOrder;
import com.sih.supplychain.domain.PurchaseOrderItem;
import com.sih.supplychain.domain.Supplier;
import com.sih.supplychain.domain.SupplierMaterial;
import com.sih.supplychain.repository.DeliveryRepository;
import com.sih.supplychain.repository.PurchaseOrderRepository;
import com.sih.supplychain.repository.SupplierMaterialRepository;
import com.sih.supplychain.service.SupplierPerformanceCalculatorService.RawPerformanceMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierPerformanceCalculatorServiceTests {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private SupplierMaterialRepository supplierMaterialRepository;

    private SupplierPerformanceCalculatorService calculatorService;

    private Supplier supplier;

    @BeforeEach
    void setUp() {
        this.calculatorService = new SupplierPerformanceCalculatorService(
                this.purchaseOrderRepository,
                this.deliveryRepository,
                this.supplierMaterialRepository
        );
        this.supplier = new Supplier("Test Supplier", "SUP-101");
        this.supplier.setLeadTimeDays(5);
        this.supplier.setCapacity(new BigDecimal("1000"));
        this.supplier.setReliabilityScore(new BigDecimal("90.00"));
    }

    @Test
    void calculatePerformance_allDeliveriesOnTime() {
        LocalDate today = LocalDate.of(2026, 9, 1);

        PurchaseOrder po = new PurchaseOrder("PO-1", this.supplier);
        po.setOrderDate(today.minusDays(10));
        po.setStatus("RECEIVED");

        Delivery d1 = createDelivery(po, "DELIVERED", today.minusDays(4), today.minusDays(5), 0); // early
        Delivery d2 = createDelivery(po, "DELIVERED", today.minusDays(2), today.minusDays(2), 0); // exactly on time

        when(this.purchaseOrderRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of(po));
        when(this.deliveryRepository.findByPurchaseOrderSupplierId(this.supplier.getId())).thenReturn(List.of(d1, d2));
        when(this.supplierMaterialRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of());

        RawPerformanceMetrics metrics = this.calculatorService.calculatePerformance(this.supplier, today);

        assertThat(metrics.onTimeDeliveryRate()).isEqualByComparingTo("100.00");
        assertThat(metrics.averageDelayDays()).isEqualByComparingTo("0.00");
        assertThat(metrics.completedDeliveries()).isEqualTo(2);
        assertThat(metrics.insufficientHistory()).isFalse();
    }

    @Test
    void calculatePerformance_allDeliveriesLate() {
        LocalDate today = LocalDate.of(2026, 9, 1);

        PurchaseOrder po = new PurchaseOrder("PO-1", this.supplier);
        po.setOrderDate(today.minusDays(20));
        po.setStatus("RECEIVED");

        Delivery d1 = createDelivery(po, "DELIVERED", today.minusDays(10), today.minusDays(6), 4); // 4 days late
        Delivery d2 = createDelivery(po, "DELIVERED", today.minusDays(8), today.minusDays(2), 6);  // 6 days late

        when(this.purchaseOrderRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of(po));
        when(this.deliveryRepository.findByPurchaseOrderSupplierId(this.supplier.getId())).thenReturn(List.of(d1, d2));
        when(this.supplierMaterialRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of());

        RawPerformanceMetrics metrics = this.calculatorService.calculatePerformance(this.supplier, today);

        assertThat(metrics.onTimeDeliveryRate()).isEqualByComparingTo("0.00");
        assertThat(metrics.averageDelayDays()).isEqualByComparingTo("5.00"); // (4 + 6) / 2 = 5.00
    }

    @Test
    void calculatePerformance_mixtureOfEarlyOnTimeLate_earlyDoesNotOffsetLate() {
        LocalDate today = LocalDate.of(2026, 9, 1);

        PurchaseOrder po = new PurchaseOrder("PO-1", this.supplier);
        po.setOrderDate(today.minusDays(20));
        po.setStatus("RECEIVED");

        Delivery d1 = createDelivery(po, "DELIVERED", today.minusDays(10), today.minusDays(12), 0); // 2 days early (delay = 0)
        Delivery d2 = createDelivery(po, "DELIVERED", today.minusDays(5), today.minusDays(5), 0);   // on time (delay = 0)
        Delivery d3 = createDelivery(po, "DELIVERED", today.minusDays(10), today.minusDays(4), 6);  // 6 days late (delay = 6)

        when(this.purchaseOrderRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of(po));
        when(this.deliveryRepository.findByPurchaseOrderSupplierId(this.supplier.getId())).thenReturn(List.of(d1, d2, d3));
        when(this.supplierMaterialRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of());

        RawPerformanceMetrics metrics = this.calculatorService.calculatePerformance(this.supplier, today);

        // 2 on-time out of 3 = 66.67%
        assertThat(metrics.onTimeDeliveryRate()).isEqualByComparingTo("66.67");
        // (0 + 0 + 6) / 3 = 2.00 days average delay
        assertThat(metrics.averageDelayDays()).isEqualByComparingTo("2.00");
    }

    @Test
    void calculatePerformance_missingDatesOrFutureDeliveries_safelyExcluded() {
        LocalDate today = LocalDate.of(2026, 9, 1);

        PurchaseOrder po = new PurchaseOrder("PO-1", this.supplier);
        po.setOrderDate(today.minusDays(10));
        po.setStatus("PLACED");

        Delivery dMissingActual = createDelivery(po, "DELIVERED", today.minusDays(5), null, null);
        Delivery dFuture = createDelivery(po, "DELIVERED", today.plusDays(5), today.plusDays(5), 0);
        Delivery dValid = createDelivery(po, "DELIVERED", today.minusDays(5), today.minusDays(5), 0);

        when(this.purchaseOrderRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of(po));
        when(this.deliveryRepository.findByPurchaseOrderSupplierId(this.supplier.getId())).thenReturn(List.of(dMissingActual, dFuture, dValid));
        when(this.supplierMaterialRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of());

        RawPerformanceMetrics metrics = this.calculatorService.calculatePerformance(this.supplier, today);

        assertThat(metrics.completedDeliveries()).isEqualTo(1);
        assertThat(metrics.onTimeDeliveryRate()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculatePerformance_leadTimeVariance_usingContractedLeadTime() {
        LocalDate today = LocalDate.of(2026, 9, 1);

        Material mat = new Material("MAT-1", "Steel");
        PurchaseOrder po = new PurchaseOrder("PO-1", this.supplier);
        po.setOrderDate(today.minusDays(15));
        po.setStatus("RECEIVED");

        PurchaseOrderItem item = new PurchaseOrderItem(po, mat, new BigDecimal("100"));
        po.getItems().add(item);

        SupplierMaterial sm = new SupplierMaterial(this.supplier, mat);
        sm.setLeadTimeDays(7); // Contracted: 7 days

        // Delivery realized lead time = actual (minusDays(5)) - order (minusDays(15)) = 10 days
        // Variance = |10 - 7| = 3 days
        Delivery d1 = createDelivery(po, "DELIVERED", today.minusDays(5), today.minusDays(5), 0);

        when(this.purchaseOrderRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of(po));
        when(this.deliveryRepository.findByPurchaseOrderSupplierId(this.supplier.getId())).thenReturn(List.of(d1));
        when(this.supplierMaterialRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of(sm));

        RawPerformanceMetrics metrics = this.calculatorService.calculatePerformance(this.supplier, today);

        assertThat(metrics.leadTimeVariance()).isEqualByComparingTo("3.00");
    }

    @Test
    void calculatePerformance_fulfillmentRate_partialAndFull() {
        LocalDate today = LocalDate.of(2026, 9, 1);

        Material mat = new Material("MAT-1", "Steel");

        PurchaseOrder po1 = new PurchaseOrder("PO-1", this.supplier);
        po1.setOrderDate(today.minusDays(10));
        po1.setStatus("RECEIVED");
        PurchaseOrderItem item1 = new PurchaseOrderItem(po1, mat, new BigDecimal("100"));
        item1.setReceivedQuantity(new BigDecimal("100"));
        po1.getItems().add(item1);

        PurchaseOrder po2 = new PurchaseOrder("PO-2", this.supplier);
        po2.setOrderDate(today.minusDays(5));
        po2.setStatus("PARTIALLY_RECEIVED");
        PurchaseOrderItem item2 = new PurchaseOrderItem(po2, mat, new BigDecimal("100"));
        item2.setReceivedQuantity(new BigDecimal("60"));
        po2.getItems().add(item2);

        when(this.purchaseOrderRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of(po1, po2));
        when(this.deliveryRepository.findByPurchaseOrderSupplierId(this.supplier.getId())).thenReturn(List.of());
        when(this.supplierMaterialRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of());

        RawPerformanceMetrics metrics = this.calculatorService.calculatePerformance(this.supplier, today);

        // Total ordered = 200, Total received = 160 -> 80.00%
        assertThat(metrics.fulfillmentRate()).isEqualByComparingTo("80.00");
    }

    @Test
    void calculatePerformance_disruptionCounting_7DayDelayAndCancellations() {
        LocalDate today = LocalDate.of(2026, 9, 1);

        PurchaseOrder po1 = new PurchaseOrder("PO-1", this.supplier);
        po1.setOrderDate(today.minusDays(30));
        po1.setStatus("RECEIVED");

        // 1. Delivery with exactly 7 days delay -> Disruption #1
        Delivery dCritical = createDelivery(po1, "DELIVERED", today.minusDays(20), today.minusDays(13), 7);
        // 2. Delivery with 6 days delay -> NOT a critical disruption
        Delivery dNonCritical = createDelivery(po1, "DELIVERED", today.minusDays(15), today.minusDays(9), 6);
        // 3. Cancelled in-transit delivery -> Disruption #2
        Delivery dCancelledTransit = createDelivery(po1, "CANCELLED", today.minusDays(10), null, null);
        dCancelledTransit.setDispatchDate(today.minusDays(12));

        // 4. Cancelled placed PO -> Disruption #3
        PurchaseOrder poCancelled = new PurchaseOrder("PO-2", this.supplier);
        poCancelled.setOrderDate(today.minusDays(10));
        poCancelled.setStatus("CANCELLED");

        when(this.purchaseOrderRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of(po1, poCancelled));
        when(this.deliveryRepository.findByPurchaseOrderSupplierId(this.supplier.getId())).thenReturn(List.of(dCritical, dNonCritical, dCancelledTransit));
        when(this.supplierMaterialRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of());

        RawPerformanceMetrics metrics = this.calculatorService.calculatePerformance(this.supplier, today);

        assertThat(metrics.disruptionCount()).isEqualTo(3);
    }

    @Test
    void calculatePerformance_zeroHistory_flagsInsufficientHistory() {
        LocalDate today = LocalDate.of(2026, 9, 1);

        when(this.purchaseOrderRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of());
        when(this.deliveryRepository.findByPurchaseOrderSupplierId(this.supplier.getId())).thenReturn(List.of());
        when(this.supplierMaterialRepository.findBySupplierId(this.supplier.getId())).thenReturn(List.of());

        RawPerformanceMetrics metrics = this.calculatorService.calculatePerformance(this.supplier, today);

        assertThat(metrics.insufficientHistory()).isTrue();
        assertThat(metrics.onTimeDeliveryRate()).isNull();
        assertThat(metrics.averageDelayDays()).isNull();
        assertThat(metrics.leadTimeVariance()).isNull();
        assertThat(metrics.fulfillmentRate()).isNull();
        assertThat(metrics.rejectionRate()).isNull();
    }

    private Delivery createDelivery(PurchaseOrder po, String status, LocalDate expected, LocalDate actual, Integer delay) {
        Delivery d = new Delivery(po, "TRK-" + System.nanoTime());
        d.setStatus(status);
        d.setExpectedArrivalDate(expected);
        d.setActualArrivalDate(actual);
        d.setDelayDays(delay);
        return d;
    }
}
