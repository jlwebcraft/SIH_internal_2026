package com.sih.supplychain.service;

import com.sih.supplychain.domain.Delivery;
import com.sih.supplychain.domain.PurchaseOrder;
import com.sih.supplychain.domain.PurchaseOrderItem;
import com.sih.supplychain.domain.Supplier;
import com.sih.supplychain.domain.SupplierMaterial;
import com.sih.supplychain.repository.DeliveryRepository;
import com.sih.supplychain.repository.PurchaseOrderRepository;
import com.sih.supplychain.repository.SupplierMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SupplierPerformanceCalculatorService {

    public static final int ROLLING_WINDOW_DAYS = 90;
    public static final int CRITICAL_DELAY_THRESHOLD_DAYS = 7;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final DeliveryRepository deliveryRepository;
    private final SupplierMaterialRepository supplierMaterialRepository;

    public SupplierPerformanceCalculatorService(
            PurchaseOrderRepository purchaseOrderRepository,
            DeliveryRepository deliveryRepository,
            SupplierMaterialRepository supplierMaterialRepository
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.deliveryRepository = deliveryRepository;
        this.supplierMaterialRepository = supplierMaterialRepository;
    }

    public RawPerformanceMetrics calculatePerformance(Supplier supplier, LocalDate evaluationDate) {
        LocalDate evalDate = evaluationDate == null ? LocalDate.now() : evaluationDate;
        LocalDate windowStart = evalDate.minusDays(ROLLING_WINDOW_DAYS);
        LocalDate windowEnd = evalDate;

        List<PurchaseOrder> allSupplierPos = this.purchaseOrderRepository.findBySupplierId(supplier.getId());
        List<Delivery> allSupplierDeliveries = this.deliveryRepository.findByPurchaseOrderSupplierId(supplier.getId());
        List<SupplierMaterial> supplierMaterials = this.supplierMaterialRepository.findBySupplierId(supplier.getId());

        Map<Long, Integer> materialLeadTimes = supplierMaterials.stream()
                .filter(sm -> sm.getMaterial() != null && sm.getLeadTimeDays() != null)
                .collect(Collectors.toMap(sm -> sm.getMaterial().getId(), SupplierMaterial::getLeadTimeDays, (a, b) -> a));

        // 1. Filter POs within window
        List<PurchaseOrder> windowPos = allSupplierPos.stream()
                .filter(po -> isPoInWindow(po, windowStart, windowEnd))
                .toList();

        // 2. Filter Deliveries within window
        List<Delivery> windowDeliveries = allSupplierDeliveries.stream()
                .filter(d -> isDeliveryInWindow(d, windowStart, windowEnd))
                .toList();

        // 3. Completed Deliveries (DELIVERED with actual & expected arrival dates)
        List<Delivery> completedDeliveries = windowDeliveries.stream()
                .filter(d -> "DELIVERED".equalsIgnoreCase(d.getStatus())
                        && d.getActualArrivalDate() != null
                        && d.getExpectedArrivalDate() != null
                        && !d.getActualArrivalDate().isAfter(windowEnd))
                .toList();

        // Check if history is sufficient
        boolean insufficientHistory = completedDeliveries.isEmpty() && windowPos.stream()
                .noneMatch(po -> !"DRAFT".equalsIgnoreCase(po.getStatus()) && !"CANCELLED".equalsIgnoreCase(po.getStatus()));

        // Metrics calculation
        BigDecimal onTimeDeliveryRate = calculateOtdr(completedDeliveries);
        BigDecimal averageDelayDays = calculateAverageDelay(completedDeliveries);
        BigDecimal leadTimeVariance = calculateLeadTimeVariance(completedDeliveries, materialLeadTimes, supplier.getLeadTimeDays());
        BigDecimal fulfillmentRate = calculateFulfillmentRate(windowPos);
        BigDecimal capacityUtilization = calculateCapacityUtilization(windowPos, supplier.getCapacity());
        Integer disruptionCount = calculateDisruptionCount(windowDeliveries, windowPos);

        return new RawPerformanceMetrics(
                supplier,
                evalDate,
                ROLLING_WINDOW_DAYS,
                windowStart,
                windowEnd,
                onTimeDeliveryRate,
                averageDelayDays,
                leadTimeVariance,
                fulfillmentRate,
                null, // Rejection rate is null in Phase 7
                capacityUtilization,
                disruptionCount,
                insufficientHistory,
                windowPos.size(),
                completedDeliveries.size()
        );
    }

    private boolean isPoInWindow(PurchaseOrder po, LocalDate start, LocalDate end) {
        LocalDate date = po.getOrderDate() != null ? po.getOrderDate() : po.getExpectedDeliveryDate();
        if (date == null) {
            return false;
        }
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private boolean isDeliveryInWindow(Delivery delivery, LocalDate start, LocalDate end) {
        LocalDate date = delivery.getActualArrivalDate() != null
                ? delivery.getActualArrivalDate()
                : (delivery.getExpectedArrivalDate() != null ? delivery.getExpectedArrivalDate() : delivery.getDispatchDate());
        if (date == null) {
            return false;
        }
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private BigDecimal calculateOtdr(List<Delivery> completedDeliveries) {
        if (completedDeliveries.isEmpty()) {
            return null;
        }
        long onTimeCount = completedDeliveries.stream()
                .filter(d -> !d.getActualArrivalDate().isAfter(d.getExpectedArrivalDate()))
                .count();

        return BigDecimal.valueOf(onTimeCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(completedDeliveries.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverageDelay(List<Delivery> completedDeliveries) {
        if (completedDeliveries.isEmpty()) {
            return null;
        }
        long totalDelayDays = 0;
        for (Delivery d : completedDeliveries) {
            if (d.getActualArrivalDate().isAfter(d.getExpectedArrivalDate())) {
                totalDelayDays += ChronoUnit.DAYS.between(d.getExpectedArrivalDate(), d.getActualArrivalDate());
            }
        }
        return BigDecimal.valueOf(totalDelayDays)
                .divide(BigDecimal.valueOf(completedDeliveries.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLeadTimeVariance(
            List<Delivery> completedDeliveries,
            Map<Long, Integer> materialLeadTimes,
            Integer defaultSupplierLeadTime
    ) {
        List<Long> absoluteVariances = new ArrayList<>();

        for (Delivery d : completedDeliveries) {
            PurchaseOrder po = d.getPurchaseOrder();
            if (po == null || po.getOrderDate() == null || d.getActualArrivalDate() == null) {
                continue;
            }
            long actualLeadTime = ChronoUnit.DAYS.between(po.getOrderDate(), d.getActualArrivalDate());
            if (actualLeadTime < 0) {
                continue;
            }

            Integer contracted = null;
            if (po.getItems() != null && !po.getItems().isEmpty()) {
                for (PurchaseOrderItem item : po.getItems()) {
                    if (item.getMaterial() != null && materialLeadTimes.containsKey(item.getMaterial().getId())) {
                        contracted = materialLeadTimes.get(item.getMaterial().getId());
                        break;
                    }
                }
            }
            if (contracted == null) {
                contracted = defaultSupplierLeadTime;
            }

            if (contracted != null) {
                absoluteVariances.add(Math.abs(actualLeadTime - contracted));
            }
        }

        if (absoluteVariances.isEmpty()) {
            return null;
        }

        long sum = absoluteVariances.stream().mapToLong(Long::longValue).sum();
        return BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(absoluteVariances.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFulfillmentRate(List<PurchaseOrder> windowPos) {
        List<PurchaseOrder> activePos = windowPos.stream()
                .filter(po -> "PLACED".equalsIgnoreCase(po.getStatus())
                        || "PARTIALLY_RECEIVED".equalsIgnoreCase(po.getStatus())
                        || "RECEIVED".equalsIgnoreCase(po.getStatus()))
                .toList();

        if (activePos.isEmpty()) {
            return null;
        }

        BigDecimal totalOrdered = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;

        for (PurchaseOrder po : activePos) {
            if (po.getItems() == null || po.getItems().isEmpty()) {
                continue;
            }
            boolean isFullyReceived = "RECEIVED".equalsIgnoreCase(po.getStatus());
            for (PurchaseOrderItem item : po.getItems()) {
                if (item.getQuantity() != null) {
                    totalOrdered = totalOrdered.add(item.getQuantity());
                    if (item.getReceivedQuantity() != null) {
                        totalReceived = totalReceived.add(item.getReceivedQuantity());
                    } else if (isFullyReceived) {
                        totalReceived = totalReceived.add(item.getQuantity());
                    }
                }
            }
        }

        if (totalOrdered.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal rate = totalReceived.multiply(BigDecimal.valueOf(100))
                .divide(totalOrdered, 2, RoundingMode.HALF_UP);
        return rate.min(BigDecimal.valueOf(100.00));
    }

    private BigDecimal calculateCapacityUtilization(List<PurchaseOrder> windowPos, BigDecimal supplierCapacity) {
        if (supplierCapacity == null || supplierCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal activeOrderedQuantity = BigDecimal.ZERO;
        for (PurchaseOrder po : windowPos) {
            if (!"CANCELLED".equalsIgnoreCase(po.getStatus()) && po.getItems() != null) {
                for (PurchaseOrderItem item : po.getItems()) {
                    if (item.getQuantity() != null) {
                        activeOrderedQuantity = activeOrderedQuantity.add(item.getQuantity());
                    }
                }
            }
        }

        return activeOrderedQuantity.multiply(BigDecimal.valueOf(100))
                .divide(supplierCapacity, 2, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100.00));
    }

    private Integer calculateDisruptionCount(List<Delivery> windowDeliveries, List<PurchaseOrder> windowPos) {
        Set<String> disruptionEvents = new HashSet<>();

        // 1. Critical delivery delays (>= 7 days)
        for (Delivery d : windowDeliveries) {
            if (d.getActualArrivalDate() != null && d.getExpectedArrivalDate() != null) {
                long delay = ChronoUnit.DAYS.between(d.getExpectedArrivalDate(), d.getActualArrivalDate());
                if (delay >= CRITICAL_DELAY_THRESHOLD_DAYS) {
                    disruptionEvents.add("DELIVERY_CRITICAL_DELAY_" + d.getId());
                }
            } else if (d.getDelayDays() != null && d.getDelayDays() >= CRITICAL_DELAY_THRESHOLD_DAYS) {
                disruptionEvents.add("DELIVERY_CRITICAL_DELAY_" + d.getId());
            }
        }

        // 2. Cancelled in-transit shipments
        for (Delivery d : windowDeliveries) {
            if ("CANCELLED".equalsIgnoreCase(d.getStatus()) && d.getDispatchDate() != null) {
                disruptionEvents.add("DELIVERY_TRANSIT_CANCEL_" + d.getId());
            }
        }

        // 3. Cancelled placed POs
        for (PurchaseOrder po : windowPos) {
            if ("CANCELLED".equalsIgnoreCase(po.getStatus()) && po.getOrderDate() != null) {
                disruptionEvents.add("PO_CANCELLED_" + po.getId());
            }
        }

        return disruptionEvents.size();
    }

    public record RawPerformanceMetrics(
            Supplier supplier,
            LocalDate evaluationDate,
            int windowDays,
            LocalDate windowStartDate,
            LocalDate windowEndDate,
            BigDecimal onTimeDeliveryRate,
            BigDecimal averageDelayDays,
            BigDecimal leadTimeVariance,
            BigDecimal fulfillmentRate,
            BigDecimal rejectionRate,
            BigDecimal capacityUtilization,
            Integer disruptionCount,
            boolean insufficientHistory,
            int totalOrders,
            int completedDeliveries
    ) {
    }
}
