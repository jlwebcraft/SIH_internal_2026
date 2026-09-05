package com.sih.supplychain.service;

import com.sih.supplychain.domain.Material;
import com.sih.supplychain.domain.PurchaseOrder;
import com.sih.supplychain.domain.PurchaseOrderItem;
import com.sih.supplychain.domain.Supplier;
import com.sih.supplychain.exception.DuplicateResourceException;
import com.sih.supplychain.exception.InvalidBusinessStateException;
import com.sih.supplychain.exception.ResourceNotFoundException;
import com.sih.supplychain.repository.MaterialRepository;
import com.sih.supplychain.repository.PurchaseOrderItemRepository;
import com.sih.supplychain.repository.PurchaseOrderRepository;
import com.sih.supplychain.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PurchaseOrderService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PLACED = "PLACED";
    public static final String STATUS_PARTIALLY_RECEIVED = "PARTIALLY_RECEIVED";
    public static final String STATUS_RECEIVED = "RECEIVED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private static final String ITEM_STATUS_OPEN = "OPEN";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            SupplierRepository supplierRepository,
            MaterialRepository materialRepository
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.supplierRepository = supplierRepository;
        this.materialRepository = materialRepository;
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(CreatePurchaseOrderCommand command) {
        validateCreateCommand(command);
        Supplier supplier = getSupplier(command.supplierId());
        if (this.purchaseOrderRepository.existsByPoNumber(command.poNumber())) {
            throw new DuplicateResourceException("Purchase order number already exists: " + command.poNumber());
        }

        LocalDate orderDate = command.orderDate() == null ? LocalDate.now() : command.orderDate();
        validateExpectedDate(orderDate, command.expectedDeliveryDate(), "Purchase order expected delivery date");

        List<ResolvedPurchaseOrderItem> resolvedItems = command.items().stream()
                .map(item -> resolveItem(item, orderDate, command.expectedDeliveryDate()))
                .toList();

        PurchaseOrder purchaseOrder = new PurchaseOrder(command.poNumber(), supplier);
        purchaseOrder.setOrderDate(orderDate);
        purchaseOrder.setExpectedDeliveryDate(command.expectedDeliveryDate());
        purchaseOrder.setStatus(STATUS_DRAFT);
        purchaseOrder.setTotalAmount(calculateTotal(resolvedItems));

        PurchaseOrder savedOrder = this.purchaseOrderRepository.save(purchaseOrder);
        List<PurchaseOrderItem> savedItems = resolvedItems.stream()
                .map(item -> toPurchaseOrderItem(savedOrder, item))
                .toList();
        this.purchaseOrderItemRepository.saveAll(savedItems);
        savedOrder.getItems().addAll(savedItems);
        return savedOrder;
    }

    public PurchaseOrder getPurchaseOrderById(Long id) {
        return this.purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));
    }

    public PurchaseOrder getPurchaseOrderByNumber(String poNumber) {
        BusinessValidation.requireText(poNumber, "Purchase order number");
        return this.purchaseOrderRepository.findByPoNumber(poNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with number: " + poNumber));
    }

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return this.purchaseOrderRepository.findAll();
    }

    public List<PurchaseOrder> getPurchaseOrdersBySupplier(Long supplierId) {
        getSupplier(supplierId);
        return this.purchaseOrderRepository.findBySupplierId(supplierId);
    }

    public List<PurchaseOrder> getPurchaseOrdersByStatus(String status) {
        validateKnownPurchaseOrderStatus(status);
        return this.purchaseOrderRepository.findByStatus(status);
    }

    @Transactional
    public PurchaseOrder updatePurchaseOrder(Long id, UpdatePurchaseOrderCommand command) {
        if (command == null) {
            throw new InvalidBusinessStateException("Purchase order changes are required");
        }
        PurchaseOrder purchaseOrder = getPurchaseOrderById(id);
        if (!STATUS_DRAFT.equals(purchaseOrder.getStatus()) && !STATUS_PLACED.equals(purchaseOrder.getStatus())) {
            throw new InvalidBusinessStateException("Only draft or placed purchase orders can be updated");
        }
        validateExpectedDate(
                purchaseOrder.getOrderDate(),
                command.expectedDeliveryDate(),
                "Purchase order expected delivery date"
        );
        purchaseOrder.setExpectedDeliveryDate(command.expectedDeliveryDate());
        return this.purchaseOrderRepository.save(purchaseOrder);
    }

    @Transactional
    public PurchaseOrder placePurchaseOrder(Long id) {
        PurchaseOrder purchaseOrder = getPurchaseOrderById(id);
        requireTransition(purchaseOrder.getStatus(), STATUS_PLACED, STATUS_DRAFT);
        purchaseOrder.setStatus(STATUS_PLACED);
        return this.purchaseOrderRepository.save(purchaseOrder);
    }

    @Transactional
    public PurchaseOrder cancelPurchaseOrder(Long id) {
        PurchaseOrder purchaseOrder = getPurchaseOrderById(id);
        requireTransition(purchaseOrder.getStatus(), STATUS_CANCELLED, STATUS_DRAFT, STATUS_PLACED);
        purchaseOrder.setStatus(STATUS_CANCELLED);
        return this.purchaseOrderRepository.save(purchaseOrder);
    }

    private void validateCreateCommand(CreatePurchaseOrderCommand command) {
        if (command == null) {
            throw new InvalidBusinessStateException("Purchase order details are required");
        }
        BusinessValidation.requireText(command.poNumber(), "Purchase order number");
        if (command.supplierId() == null) {
            throw new InvalidBusinessStateException("Supplier id is required");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new InvalidBusinessStateException("Purchase order must contain at least one item");
        }
    }

    private ResolvedPurchaseOrderItem resolveItem(
            PurchaseOrderItemCommand item,
            LocalDate orderDate,
            LocalDate expectedDeliveryDate
    ) {
        if (item == null) {
            throw new InvalidBusinessStateException("Purchase order item is required");
        }
        if (item.materialId() == null) {
            throw new InvalidBusinessStateException("Material id is required");
        }
        BusinessValidation.requirePositive(item.quantity(), "Purchase order item quantity");
        BusinessValidation.requireNonNegative(item.unitPrice(), "Purchase order item unit price");
        if (item.unitPrice() == null) {
            throw new InvalidBusinessStateException("Purchase order item unit price is required");
        }
        validateExpectedDate(orderDate, item.expectedDate(), "Purchase order item expected date");
        if (expectedDeliveryDate != null && item.expectedDate() != null && item.expectedDate().isAfter(expectedDeliveryDate)) {
            throw new InvalidBusinessStateException("Purchase order item expected date cannot be after purchase order expected delivery date");
        }
        Material material = getMaterial(item.materialId());
        return new ResolvedPurchaseOrderItem(material, item.quantity(), item.unitPrice(), item.expectedDate());
    }

    private PurchaseOrderItem toPurchaseOrderItem(PurchaseOrder purchaseOrder, ResolvedPurchaseOrderItem item) {
        PurchaseOrderItem purchaseOrderItem = new PurchaseOrderItem(purchaseOrder, item.material(), item.quantity());
        purchaseOrderItem.setUnitPrice(item.unitPrice());
        purchaseOrderItem.setExpectedDate(item.expectedDate());
        purchaseOrderItem.setReceivedQuantity(BigDecimal.ZERO);
        purchaseOrderItem.setStatus(ITEM_STATUS_OPEN);
        return purchaseOrderItem;
    }

    private BigDecimal calculateTotal(List<ResolvedPurchaseOrderItem> items) {
        return items.stream()
                .map(item -> item.quantity().multiply(item.unitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateExpectedDate(LocalDate baseDate, LocalDate expectedDate, String fieldName) {
        if (baseDate != null && expectedDate != null && expectedDate.isBefore(baseDate)) {
            throw new InvalidBusinessStateException(fieldName + " cannot be before order date");
        }
    }

    private void validateKnownPurchaseOrderStatus(String status) {
        BusinessValidation.requireText(status, "Purchase order status");
        if (!STATUS_DRAFT.equals(status)
                && !STATUS_PLACED.equals(status)
                && !STATUS_PARTIALLY_RECEIVED.equals(status)
                && !STATUS_RECEIVED.equals(status)
                && !STATUS_CANCELLED.equals(status)) {
            throw new InvalidBusinessStateException("Unknown purchase order status: " + status);
        }
    }

    private void requireTransition(String currentStatus, String targetStatus, String... allowedCurrentStatuses) {
        for (String allowedStatus : allowedCurrentStatuses) {
            if (allowedStatus.equals(currentStatus)) {
                return;
            }
        }
        throw new InvalidBusinessStateException(
                "Purchase order cannot transition from " + currentStatus + " to " + targetStatus
        );
    }

    private Supplier getSupplier(Long supplierId) {
        return this.supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
    }

    private Material getMaterial(Long materialId) {
        return this.materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + materialId));
    }

    public record CreatePurchaseOrderCommand(
            String poNumber,
            Long supplierId,
            LocalDate orderDate,
            LocalDate expectedDeliveryDate,
            List<PurchaseOrderItemCommand> items
    ) {
    }

    public record UpdatePurchaseOrderCommand(LocalDate expectedDeliveryDate) {
    }

    public record PurchaseOrderItemCommand(
            Long materialId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            LocalDate expectedDate
    ) {
    }

    private record ResolvedPurchaseOrderItem(
            Material material,
            BigDecimal quantity,
            BigDecimal unitPrice,
            LocalDate expectedDate
    ) {
    }
}
