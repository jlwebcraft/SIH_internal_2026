package com.sih.supplychain.mapper;

import com.sih.supplychain.domain.CustomerOrder;
import com.sih.supplychain.domain.CustomerOrderItem;
import com.sih.supplychain.domain.Delivery;
import com.sih.supplychain.domain.Inventory;
import com.sih.supplychain.domain.Material;
import com.sih.supplychain.domain.Product;
import com.sih.supplychain.domain.ProductMaterial;
import com.sih.supplychain.domain.ProductionOrder;
import com.sih.supplychain.domain.PurchaseOrder;
import com.sih.supplychain.domain.PurchaseOrderItem;
import com.sih.supplychain.domain.Supplier;
import com.sih.supplychain.domain.SupplierMaterial;
import com.sih.supplychain.domain.User;
import com.sih.supplychain.dto.common.MaterialSummaryResponse;
import com.sih.supplychain.dto.common.ProductSummaryResponse;
import com.sih.supplychain.dto.common.PurchaseOrderSummaryResponse;
import com.sih.supplychain.dto.common.SupplierSummaryResponse;
import com.sih.supplychain.dto.common.UserSummaryResponse;
import com.sih.supplychain.dto.customerorder.CustomerOrderItemResponse;
import com.sih.supplychain.dto.customerorder.CustomerOrderResponse;
import com.sih.supplychain.dto.delivery.DeliveryCreateRequest;
import com.sih.supplychain.dto.delivery.DeliveryResponse;
import com.sih.supplychain.dto.delivery.DeliveryUpdateRequest;
import com.sih.supplychain.dto.inventory.InventoryCreateRequest;
import com.sih.supplychain.dto.inventory.InventoryResponse;
import com.sih.supplychain.dto.inventory.InventoryUpdateRequest;
import com.sih.supplychain.dto.material.MaterialCreateRequest;
import com.sih.supplychain.dto.material.MaterialResponse;
import com.sih.supplychain.dto.material.MaterialUpdateRequest;
import com.sih.supplychain.dto.product.ProductCreateRequest;
import com.sih.supplychain.dto.product.ProductResponse;
import com.sih.supplychain.dto.product.ProductUpdateRequest;
import com.sih.supplychain.dto.productionorder.ProductionOrderResponse;
import com.sih.supplychain.dto.productmaterial.ProductMaterialCreateRequest;
import com.sih.supplychain.dto.productmaterial.ProductMaterialResponse;
import com.sih.supplychain.dto.productmaterial.ProductMaterialUpdateRequest;
import com.sih.supplychain.dto.purchaseorder.PurchaseOrderItemResponse;
import com.sih.supplychain.dto.purchaseorder.PurchaseOrderResponse;
import com.sih.supplychain.dto.supplier.SupplierCreateRequest;
import com.sih.supplychain.dto.supplier.SupplierResponse;
import com.sih.supplychain.dto.supplier.SupplierUpdateRequest;
import com.sih.supplychain.dto.suppliermaterial.SupplierMaterialCreateRequest;
import com.sih.supplychain.dto.suppliermaterial.SupplierMaterialResponse;
import com.sih.supplychain.dto.suppliermaterial.SupplierMaterialUpdateRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

public final class OperationalMapper {

    private OperationalMapper() {
    }

    public static Supplier toSupplier(SupplierCreateRequest request) {
        Supplier supplier = new Supplier(request.name(), request.code());
        applySupplierFields(supplier, request);
        return supplier;
    }

    public static Supplier toSupplier(SupplierUpdateRequest request) {
        Supplier supplier = new Supplier(request.name(), request.code());
        applySupplierFields(supplier, request);
        return supplier;
    }

    public static SupplierResponse toSupplierResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getCode(),
                supplier.getContactPerson(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.getCity(),
                supplier.getState(),
                supplier.getCountry(),
                supplier.getLeadTimeDays(),
                supplier.getCapacity(),
                supplier.getReliabilityScore(),
                supplier.getStatus(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }

    public static Material toMaterial(MaterialCreateRequest request) {
        Material material = new Material(request.code(), request.name());
        applyMaterialFields(material, request);
        return material;
    }

    public static Material toMaterial(MaterialUpdateRequest request) {
        Material material = new Material(request.code(), request.name());
        applyMaterialFields(material, request);
        return material;
    }

    public static MaterialResponse toMaterialResponse(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getCode(),
                material.getName(),
                material.getDescription(),
                material.getCategory(),
                material.getUnit(),
                material.getUnitCost(),
                material.getCriticality(),
                material.getCurrentStock(),
                material.getSafetyStock(),
                material.getReorderPoint(),
                material.getDailyConsumption(),
                material.getStatus(),
                material.getCreatedAt(),
                material.getUpdatedAt()
        );
    }

    public static Product toProduct(ProductCreateRequest request) {
        Product product = new Product(request.code(), request.name());
        applyProductFields(product, request);
        return product;
    }

    public static Product toProduct(ProductUpdateRequest request) {
        Product product = new Product(request.code(), request.name());
        applyProductFields(product, request);
        return product;
    }

    public static ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getUnitCost(),
                product.getSellingPrice(),
                product.getProductionTimeHours(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public static SupplierMaterial toSupplierMaterial(SupplierMaterialCreateRequest request) {
        SupplierMaterial supplierMaterial = new SupplierMaterial(request.unitPrice());
        applySupplierMaterialFields(supplierMaterial, request);
        return supplierMaterial;
    }

    public static SupplierMaterial toSupplierMaterial(SupplierMaterialUpdateRequest request) {
        SupplierMaterial supplierMaterial = new SupplierMaterial(request.unitPrice());
        applySupplierMaterialFields(supplierMaterial, request);
        return supplierMaterial;
    }

    public static SupplierMaterialResponse toSupplierMaterialResponse(SupplierMaterial supplierMaterial) {
        return new SupplierMaterialResponse(
                supplierMaterial.getId(),
                toSupplierSummary(supplierMaterial.getSupplier()),
                toMaterialSummary(supplierMaterial.getMaterial()),
                supplierMaterial.getUnitPrice(),
                supplierMaterial.getLeadTimeDays(),
                supplierMaterial.getMinimumOrderQuantity(),
                supplierMaterial.getMaximumCapacity(),
                supplierMaterial.getReliabilityScore(),
                supplierMaterial.getStatus(),
                supplierMaterial.getCreatedAt(),
                supplierMaterial.getUpdatedAt()
        );
    }

    public static ProductMaterial toProductMaterial(ProductMaterialCreateRequest request) {
        ProductMaterial productMaterial = new ProductMaterial(request.quantityRequired());
        applyProductMaterialFields(productMaterial, request);
        return productMaterial;
    }

    public static ProductMaterial toProductMaterial(ProductMaterialUpdateRequest request) {
        ProductMaterial productMaterial = new ProductMaterial(request.quantityRequired());
        applyProductMaterialFields(productMaterial, request);
        return productMaterial;
    }

    public static ProductMaterialResponse toProductMaterialResponse(ProductMaterial productMaterial) {
        return new ProductMaterialResponse(
                productMaterial.getId(),
                toProductSummary(productMaterial.getProduct()),
                toMaterialSummary(productMaterial.getMaterial()),
                productMaterial.getQuantityRequired(),
                productMaterial.getUnit(),
                productMaterial.getWastagePercentage()
        );
    }

    public static Inventory toInventory(InventoryCreateRequest request) {
        Inventory inventory = new Inventory(request.warehouseLocation());
        applyInventoryFields(inventory, request);
        return inventory;
    }

    public static Inventory toInventory(InventoryUpdateRequest request) {
        Inventory inventory = new Inventory((String) null);
        applyInventoryFields(inventory, request);
        return inventory;
    }

    public static InventoryResponse toInventoryResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                toMaterialSummary(inventory.getMaterial()),
                inventory.getWarehouseLocation(),
                inventory.getQuantityOnHand(),
                inventory.getQuantityReserved(),
                inventory.getQuantityIncoming(),
                inventory.getSafetyStock(),
                inventory.getReorderPoint(),
                inventory.getLastUpdated()
        );
    }

    public static PurchaseOrderResponse toPurchaseOrderResponse(PurchaseOrder purchaseOrder) {
        return new PurchaseOrderResponse(
                purchaseOrder.getId(),
                purchaseOrder.getPoNumber(),
                toSupplierSummary(purchaseOrder.getSupplier()),
                purchaseOrder.getStatus(),
                purchaseOrder.getOrderDate(),
                purchaseOrder.getExpectedDeliveryDate(),
                purchaseOrder.getActualDeliveryDate(),
                purchaseOrder.getTotalAmount(),
                purchaseOrder.getItems().stream()
                        .sorted(Comparator.comparing(PurchaseOrderItem::getId, Comparator.nullsLast(Long::compareTo)))
                        .map(OperationalMapper::toPurchaseOrderItemResponse)
                        .toList(),
                purchaseOrder.getCreatedAt(),
                purchaseOrder.getUpdatedAt()
        );
    }

    public static PurchaseOrderItemResponse toPurchaseOrderItemResponse(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponse(
                item.getId(),
                toMaterialSummary(item.getMaterial()),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getExpectedDate(),
                item.getReceivedQuantity(),
                item.getStatus(),
                calculateLineAmount(item)
        );
    }

    public static Delivery toDelivery(DeliveryCreateRequest request) {
        Delivery delivery = new Delivery(null, request.trackingNumber());
        applyDeliveryFields(delivery, request);
        return delivery;
    }

    public static Delivery toDelivery(DeliveryUpdateRequest request) {
        Delivery delivery = new Delivery(null, request.trackingNumber());
        applyDeliveryFields(delivery, request);
        return delivery;
    }

    public static DeliveryResponse toDeliveryResponse(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                toPurchaseOrderSummary(delivery.getPurchaseOrder()),
                delivery.getTrackingNumber(),
                delivery.getDispatchDate(),
                delivery.getExpectedArrivalDate(),
                delivery.getActualArrivalDate(),
                delivery.getStatus(),
                delivery.getDelayDays(),
                delivery.getNotes()
        );
    }

    private static SupplierSummaryResponse toSupplierSummary(Supplier supplier) {
        return new SupplierSummaryResponse(supplier.getId(), supplier.getCode(), supplier.getName());
    }

    private static MaterialSummaryResponse toMaterialSummary(Material material) {
        return new MaterialSummaryResponse(material.getId(), material.getCode(), material.getName());
    }

    private static ProductSummaryResponse toProductSummary(Product product) {
        return new ProductSummaryResponse(product.getId(), product.getCode(), product.getName());
    }

    private static PurchaseOrderSummaryResponse toPurchaseOrderSummary(PurchaseOrder purchaseOrder) {
        return new PurchaseOrderSummaryResponse(
                purchaseOrder.getId(),
                purchaseOrder.getPoNumber(),
                purchaseOrder.getStatus()
        );
    }

    private static void applySupplierFields(Supplier supplier, SupplierCreateRequest request) {
        supplier.setContactPerson(request.contactPerson());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        supplier.setCity(request.city());
        supplier.setState(request.state());
        supplier.setCountry(request.country());
        supplier.setLeadTimeDays(request.leadTimeDays());
        supplier.setCapacity(request.capacity());
        supplier.setReliabilityScore(request.reliabilityScore());
        supplier.setStatus(request.status());
    }

    private static void applySupplierFields(Supplier supplier, SupplierUpdateRequest request) {
        supplier.setContactPerson(request.contactPerson());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        supplier.setCity(request.city());
        supplier.setState(request.state());
        supplier.setCountry(request.country());
        supplier.setLeadTimeDays(request.leadTimeDays());
        supplier.setCapacity(request.capacity());
        supplier.setReliabilityScore(request.reliabilityScore());
        supplier.setStatus(request.status());
    }

    private static void applyMaterialFields(Material material, MaterialCreateRequest request) {
        material.setDescription(request.description());
        material.setCategory(request.category());
        material.setUnit(request.unit());
        material.setUnitCost(request.unitCost());
        material.setCriticality(request.criticality());
        material.setCurrentStock(request.currentStock());
        material.setSafetyStock(request.safetyStock());
        material.setReorderPoint(request.reorderPoint());
        material.setDailyConsumption(request.dailyConsumption());
        material.setStatus(request.status());
    }

    private static void applyMaterialFields(Material material, MaterialUpdateRequest request) {
        material.setDescription(request.description());
        material.setCategory(request.category());
        material.setUnit(request.unit());
        material.setUnitCost(request.unitCost());
        material.setCriticality(request.criticality());
        material.setCurrentStock(request.currentStock());
        material.setSafetyStock(request.safetyStock());
        material.setReorderPoint(request.reorderPoint());
        material.setDailyConsumption(request.dailyConsumption());
        material.setStatus(request.status());
    }

    private static void applyProductFields(Product product, ProductCreateRequest request) {
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setUnitCost(request.unitCost());
        product.setSellingPrice(request.sellingPrice());
        product.setProductionTimeHours(request.productionTimeHours());
        product.setStatus(request.status());
    }

    private static void applyProductFields(Product product, ProductUpdateRequest request) {
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setUnitCost(request.unitCost());
        product.setSellingPrice(request.sellingPrice());
        product.setProductionTimeHours(request.productionTimeHours());
        product.setStatus(request.status());
    }

    private static void applySupplierMaterialFields(
            SupplierMaterial supplierMaterial,
            SupplierMaterialCreateRequest request
    ) {
        supplierMaterial.setLeadTimeDays(request.leadTimeDays());
        supplierMaterial.setMinimumOrderQuantity(request.minimumOrderQuantity());
        supplierMaterial.setMaximumCapacity(request.maximumCapacity());
        supplierMaterial.setReliabilityScore(request.reliabilityScore());
        supplierMaterial.setStatus(request.status());
    }

    private static void applySupplierMaterialFields(
            SupplierMaterial supplierMaterial,
            SupplierMaterialUpdateRequest request
    ) {
        supplierMaterial.setLeadTimeDays(request.leadTimeDays());
        supplierMaterial.setMinimumOrderQuantity(request.minimumOrderQuantity());
        supplierMaterial.setMaximumCapacity(request.maximumCapacity());
        supplierMaterial.setReliabilityScore(request.reliabilityScore());
        supplierMaterial.setStatus(request.status());
    }

    private static void applyProductMaterialFields(ProductMaterial productMaterial, ProductMaterialCreateRequest request) {
        productMaterial.setUnit(request.unit());
        productMaterial.setWastagePercentage(request.wastagePercentage());
    }

    private static void applyProductMaterialFields(ProductMaterial productMaterial, ProductMaterialUpdateRequest request) {
        productMaterial.setUnit(request.unit());
        productMaterial.setWastagePercentage(request.wastagePercentage());
    }

    private static void applyInventoryFields(Inventory inventory, InventoryCreateRequest request) {
        inventory.setQuantityOnHand(request.quantityOnHand());
        inventory.setQuantityReserved(request.quantityReserved());
        inventory.setQuantityIncoming(request.quantityIncoming());
        inventory.setSafetyStock(request.safetyStock());
        inventory.setReorderPoint(request.reorderPoint());
    }

    private static void applyInventoryFields(Inventory inventory, InventoryUpdateRequest request) {
        inventory.setQuantityOnHand(request.quantityOnHand());
        inventory.setQuantityReserved(request.quantityReserved());
        inventory.setQuantityIncoming(request.quantityIncoming());
        inventory.setSafetyStock(request.safetyStock());
        inventory.setReorderPoint(request.reorderPoint());
    }

    private static void applyDeliveryFields(Delivery delivery, DeliveryCreateRequest request) {
        delivery.setDispatchDate(request.dispatchDate());
        delivery.setExpectedArrivalDate(request.expectedArrivalDate());
        delivery.setActualArrivalDate(request.actualArrivalDate());
        delivery.setDelayDays(request.delayDays());
        delivery.setNotes(request.notes());
    }

    private static void applyDeliveryFields(Delivery delivery, DeliveryUpdateRequest request) {
        delivery.setDispatchDate(request.dispatchDate());
        delivery.setExpectedArrivalDate(request.expectedArrivalDate());
        delivery.setActualArrivalDate(request.actualArrivalDate());
        delivery.setDelayDays(request.delayDays());
        delivery.setNotes(request.notes());
    }

    public static ProductionOrderResponse toProductionOrderResponse(ProductionOrder order) {
        return new ProductionOrderResponse(
                order.getId(),
                order.getProductionNumber(),
                toProductSummary(order.getProduct()),
                order.getQuantity(),
                order.getPlannedStartDate(),
                order.getPlannedEndDate(),
                order.getActualStartDate(),
                order.getActualEndDate(),
                order.getStatus(),
                order.getPriority(),
                toUserSummary(order.getCreatedBy())
        );
    }

    public static CustomerOrderResponse toCustomerOrderResponse(CustomerOrder order) {
        return new CustomerOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getOrderDate(),
                order.getRequiredDeliveryDate(),
                order.getStatus(),
                order.getPriority(),
                order.getTotalAmount(),
                order.getItems().stream()
                        .sorted(Comparator.comparing(CustomerOrderItem::getId, Comparator.nullsLast(Long::compareTo)))
                        .map(OperationalMapper::toCustomerOrderItemResponse)
                        .toList()
        );
    }

    public static CustomerOrderItemResponse toCustomerOrderItemResponse(CustomerOrderItem item) {
        return new CustomerOrderItemResponse(
                item.getId(),
                toProductSummary(item.getProduct()),
                item.getQuantity(),
                item.getUnitPrice(),
                calculateCustomerOrderLineAmount(item)
        );
    }

    public static UserSummaryResponse toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    private static BigDecimal calculateLineAmount(PurchaseOrderItem item) {
        if (item.getQuantity() == null || item.getUnitPrice() == null) {
            return null;
        }
        return item.getQuantity()
                .multiply(item.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal calculateCustomerOrderLineAmount(CustomerOrderItem item) {
        if (item.getQuantity() == null || item.getUnitPrice() == null) {
            return null;
        }
        return item.getQuantity()
                .multiply(item.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
    }
}
