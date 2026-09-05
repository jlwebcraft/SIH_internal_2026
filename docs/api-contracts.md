# API Contracts

The operational REST API contract is implemented for suppliers, materials, products, supplier-material relationships, product BOM entries, inventory, purchase orders, purchase order items, and deliveries. OpenAPI configuration, authentication, and frontend integration are intentionally not implemented yet.

The API will use `/api/...` paths for the current SIH project scope. JPA entities must not be exposed directly from controllers.

## Current Service Operations Reviewed

- `SupplierService`: create, get by ID, get by code, list all, update, delete when safe, deactivate.
- `MaterialService`: create, get by ID, get by code, list all, update, delete when safe, deactivate.
- `SupplierMaterialService`: create supplier-material relationship, get by ID, list by supplier, list by material, update, remove.
- `ProductService`: create, get by ID, get by code, list all, update, delete when safe, deactivate.
- `ProductMaterialService`: add material to product BOM, get BOM entry by ID, list product BOM, find products using material, update BOM entry, remove BOM entry.
- `InventoryService`: create inventory, get by ID, list by material, get by material and warehouse, list all, update, adjust stock.
- `PurchaseOrderService`: create purchase order with items atomically, get by ID, get by PO number, list all, list by supplier, list by status, update expected delivery date, place, cancel.
- `DeliveryService`: create delivery, get by ID, list all, list by purchase order, update details, dispatch, mark in transit, mark delayed, mark delivered, cancel.

REST controllers use DTOs, explicit mapping, and global exception handling above these services.

## Shared HTTP Semantics

- Successful reads return `200 OK`.
- Successful creates return `201 Created` and should include a `Location` header where practical.
- Successful updates return `200 OK` with the updated response body.
- Successful deletes/removals return `204 No Content`.
- Validation and invalid business-state failures return `400 Bad Request`.
- Missing resources return `404 Not Found`.
- Duplicate resources or uniqueness conflicts return `409 Conflict`.
- Unexpected failures return `500 Internal Server Error`.

## Error Response Contract

All API errors should use one consistent response shape:

```json
{
  "timestamp": "2026-09-04T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Supplier code already exists: SUP-001",
  "path": "/api/suppliers"
}
```

Planned exception mapping:

- `ResourceNotFoundException` -> `404 Not Found`
- `DuplicateResourceException` -> `409 Conflict`
- `InvalidBusinessStateException` -> `400 Bad Request`
- Bean Validation request failures -> `400 Bad Request`
- Unhandled exceptions -> `500 Internal Server Error`

A global `@RestControllerAdvice` should be implemented with the REST API layer, not before it.

## Relationship JSON Strategy

Responses must avoid unrestricted nested entity graphs. Relationship responses should expose deliberate summaries instead of recursive JPA structures.

Recommended summary shapes:

```json
{
  "id": 10,
  "code": "SUP-001",
  "name": "Supplier ABC"
}
```

```json
{
  "id": 20,
  "code": "MAT-001",
  "name": "Steel Sheet"
}
```

Relationship resources such as supplier-material and product BOM entries should include their own ID plus compact supplier, material, or product summaries.

## Suppliers

| Method | URI | Purpose | Request | Response | Success | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/suppliers` | List suppliers. Optional filters: `code`, `status`. | None | `List<SupplierResponse>` | `200` | `500` |
| `GET` | `/api/suppliers/{id}` | Get supplier by ID. | None | `SupplierResponse` | `200` | `404`, `500` |
| `POST` | `/api/suppliers` | Create supplier. | `SupplierCreateRequest` | `SupplierResponse` | `201` | `400`, `409`, `500` |
| `PUT` | `/api/suppliers/{id}` | Update mutable supplier fields. | `SupplierUpdateRequest` | `SupplierResponse` | `200` | `400`, `404`, `409`, `500` |
| `DELETE` | `/api/suppliers/{id}` | Delete supplier only when no dependent operational records exist. | None | None | `204` | `400`, `404`, `500` |

DTO design:

- `SupplierCreateRequest`: `name`, `code`, `contactPerson`, `email`, `phone`, `address`, `city`, `state`, `country`, `leadTimeDays`, `capacity`, `reliabilityScore`, `status`.
- `SupplierUpdateRequest`: same mutable fields as create. Generated IDs and timestamps are not accepted.
- `SupplierResponse`: `id`, `name`, `code`, `contactPerson`, `email`, `phone`, `address`, `city`, `state`, `country`, `leadTimeDays`, `capacity`, `reliabilityScore`, `status`, `createdAt`, `updatedAt`.

Validation expectations:

- Request DTO: `name` and `code` `@NotBlank`, `email` `@Email`, numeric values `@PositiveOrZero`, sensible size limits.
- Service: duplicate `code`, reliability score range, safe delete dependencies.

## Materials

| Method | URI | Purpose | Request | Response | Success | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/materials` | List materials. Optional filters: `code`, `status`, `criticality`. | None | `List<MaterialResponse>` | `200` | `500` |
| `GET` | `/api/materials/{id}` | Get material by ID. | None | `MaterialResponse` | `200` | `404`, `500` |
| `POST` | `/api/materials` | Create material. | `MaterialCreateRequest` | `MaterialResponse` | `201` | `400`, `409`, `500` |
| `PUT` | `/api/materials/{id}` | Update mutable material fields. | `MaterialUpdateRequest` | `MaterialResponse` | `200` | `400`, `404`, `409`, `500` |
| `DELETE` | `/api/materials/{id}` | Delete material only when no dependent operational records exist. | None | None | `204` | `400`, `404`, `500` |

DTO design:

- `MaterialCreateRequest`: `code`, `name`, `description`, `category`, `unit`, `unitCost`, `criticality`, `currentStock`, `safetyStock`, `reorderPoint`, `dailyConsumption`, `status`.
- `MaterialUpdateRequest`: same mutable fields as create.
- `MaterialResponse`: `id`, `code`, `name`, `description`, `category`, `unit`, `unitCost`, `criticality`, `currentStock`, `safetyStock`, `reorderPoint`, `dailyConsumption`, `status`, `createdAt`, `updatedAt`.

Validation expectations:

- Request DTO: `code` and `name` `@NotBlank`; quantity and money fields `@PositiveOrZero`.
- Service: duplicate `code`, safe delete dependencies, business invariants around non-negative stock planning fields.

## Products

| Method | URI | Purpose | Request | Response | Success | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/products` | List products. Optional filters: `code`, `status`. | None | `List<ProductResponse>` | `200` | `500` |
| `GET` | `/api/products/{id}` | Get product by ID. | None | `ProductResponse` | `200` | `404`, `500` |
| `POST` | `/api/products` | Create product. | `ProductCreateRequest` | `ProductResponse` | `201` | `400`, `409`, `500` |
| `PUT` | `/api/products/{id}` | Update mutable product fields. | `ProductUpdateRequest` | `ProductResponse` | `200` | `400`, `404`, `409`, `500` |
| `DELETE` | `/api/products/{id}` | Delete product only when no dependent operational records exist. | None | None | `204` | `400`, `404`, `500` |

DTO design:

- `ProductCreateRequest`: `code`, `name`, `description`, `category`, `unitCost`, `sellingPrice`, `productionTimeHours`, `status`.
- `ProductUpdateRequest`: same mutable fields as create.
- `ProductResponse`: `id`, `code`, `name`, `description`, `category`, `unitCost`, `sellingPrice`, `productionTimeHours`, `status`, `createdAt`, `updatedAt`.

Validation expectations:

- Request DTO: `code` and `name` `@NotBlank`; price/cost/time fields `@PositiveOrZero`.
- Service: duplicate `code`, safe delete dependencies, business invariant enforcement.

## Supplier-Material Relationships

| Method | URI | Purpose | Request | Response | Success | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/suppliers/{supplierId}/materials/{materialId}` | Add a material supplied by a supplier. | `SupplierMaterialCreateRequest` | `SupplierMaterialResponse` | `201` | `400`, `404`, `409`, `500` |
| `GET` | `/api/suppliers/{supplierId}/materials` | View materials supplied by a supplier. | None | `List<SupplierMaterialResponse>` | `200` | `404`, `500` |
| `GET` | `/api/materials/{materialId}/suppliers` | View suppliers for a material. | None | `List<SupplierMaterialResponse>` | `200` | `404`, `500` |
| `GET` | `/api/supplier-materials/{id}` | Get one supplier-material relationship. | None | `SupplierMaterialResponse` | `200` | `404`, `500` |
| `PUT` | `/api/supplier-materials/{id}` | Update relationship attributes. | `SupplierMaterialUpdateRequest` | `SupplierMaterialResponse` | `200` | `400`, `404`, `500` |
| `DELETE` | `/api/supplier-materials/{id}` | Remove relationship. | None | None | `204` | `404`, `500` |

DTO design:

- `SupplierMaterialCreateRequest`: `unitPrice`, `leadTimeDays`, `minimumOrderQuantity`, `maximumCapacity`, `reliabilityScore`, `status`. Supplier and material come from the URI.
- `SupplierMaterialUpdateRequest`: same mutable relationship attributes. Supplier/material IDs are not changed through update.
- `SupplierMaterialResponse`: `id`, `supplier` summary, `material` summary, `unitPrice`, `leadTimeDays`, `minimumOrderQuantity`, `maximumCapacity`, `reliabilityScore`, `status`, `createdAt`, `updatedAt`.

Validation expectations:

- Request DTO: numeric fields `@PositiveOrZero`; bounded score validation where practical.
- Service: supplier exists, material exists, duplicate `(supplierId, materialId)` rejected, relationship values remain valid.

## Product BOM

| Method | URI | Purpose | Request | Response | Success | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/products/{productId}/bom/materials/{materialId}` | Add material to product BOM. | `ProductMaterialCreateRequest` | `ProductMaterialResponse` | `201` | `400`, `404`, `409`, `500` |
| `GET` | `/api/products/{productId}/bom` | View product BOM. | None | `List<ProductMaterialResponse>` | `200` | `404`, `500` |
| `GET` | `/api/materials/{materialId}/products` | Find products using a material. | None | `List<ProductMaterialResponse>` | `200` | `404`, `500` |
| `GET` | `/api/product-materials/{id}` | Get one BOM entry. | None | `ProductMaterialResponse` | `200` | `404`, `500` |
| `PUT` | `/api/product-materials/{id}` | Update BOM entry attributes. | `ProductMaterialUpdateRequest` | `ProductMaterialResponse` | `200` | `400`, `404`, `500` |
| `DELETE` | `/api/product-materials/{id}` | Remove BOM entry. | None | None | `204` | `404`, `500` |

DTO design:

- `ProductMaterialCreateRequest`: `quantityRequired`, `unit`, `wastagePercentage`. Product and material come from the URI.
- `ProductMaterialUpdateRequest`: `quantityRequired`, `unit`, `wastagePercentage`.
- `ProductMaterialResponse`: `id`, `product` summary, `material` summary, `quantityRequired`, `unit`, `wastagePercentage`.

Validation expectations:

- Request DTO: `quantityRequired` `@NotNull` and `@Positive`; `wastagePercentage` `@PositiveOrZero`.
- Service: product exists, material exists, duplicate `(productId, materialId)` rejected, wastage remains within a sensible maximum.

## Inventory

| Method | URI | Purpose | Request | Response | Success | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/inventory` | List inventory. Optional filters: `materialId`, `warehouseLocation`. | None | `List<InventoryResponse>` | `200` | `500` |
| `GET` | `/api/inventory/{id}` | Get inventory by ID. | None | `InventoryResponse` | `200` | `404`, `500` |
| `GET` | `/api/materials/{materialId}/inventory` | View inventory records for a material. | None | `List<InventoryResponse>` | `200` | `404`, `500` |
| `GET` | `/api/materials/{materialId}/inventory/{warehouseLocation}` | View inventory for material and warehouse. | None | `InventoryResponse` | `200` | `404`, `500` |
| `POST` | `/api/inventory` | Create inventory record for material. | `InventoryCreateRequest` | `InventoryResponse` | `201` | `400`, `404`, `409`, `500` |
| `PUT` | `/api/inventory/{id}` | Update inventory planning/quantity fields. | `InventoryUpdateRequest` | `InventoryResponse` | `200` | `400`, `404`, `500` |
| `PATCH` | `/api/inventory/{id}/adjust` | Adjust stock by a positive or negative delta. | `InventoryAdjustmentRequest` | `InventoryResponse` | `200` | `400`, `404`, `500` |

DTO design:

- `InventoryCreateRequest`: `materialId`, `warehouseLocation`, `quantityOnHand`, `quantityReserved`, `quantityIncoming`, `safetyStock`, `reorderPoint`.
- `InventoryUpdateRequest`: `quantityOnHand`, `quantityReserved`, `quantityIncoming`, `safetyStock`, `reorderPoint`. Material and warehouse are not changed through update.
- `InventoryAdjustmentRequest`: `quantityChange`.
- `InventoryResponse`: `id`, `material` summary, `warehouseLocation`, `quantityOnHand`, `quantityReserved`, `quantityIncoming`, `safetyStock`, `reorderPoint`, `lastUpdated`.

Validation expectations:

- Request DTO: `warehouseLocation` `@NotBlank`; quantity fields `@PositiveOrZero`; `adjustment` `@NotNull`.
- Service: material exists, duplicate material/warehouse rejected, stock quantities cannot become negative, adjustment cannot make `quantityOnHand` negative.

## Purchase Orders

| Method | URI | Purpose | Request | Response | Success | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/purchase-orders` | List purchase orders. Optional filters: `poNumber`, `supplierId`, `status`. | None | `List<PurchaseOrderResponse>` | `200` | `400`, `404`, `500` |
| `GET` | `/api/purchase-orders/{id}` | Get purchase order by ID. | None | `PurchaseOrderResponse` | `200` | `404`, `500` |
| `GET` | `/api/purchase-orders/by-number/{poNumber}` | Get purchase order by PO number. | None | `PurchaseOrderResponse` | `200` | `404`, `500` |
| `POST` | `/api/purchase-orders` | Create a purchase order with one or more items. | `PurchaseOrderCreateRequest` | `PurchaseOrderResponse` | `201` | `400`, `404`, `409`, `500` |
| `PUT` | `/api/purchase-orders/{id}` | Update allowed purchase order fields. | `PurchaseOrderUpdateRequest` | `PurchaseOrderResponse` | `200` | `400`, `404`, `500` |
| `PATCH` | `/api/purchase-orders/{id}/place` | Move a draft purchase order to placed. | None | `PurchaseOrderResponse` | `200` | `400`, `404`, `500` |
| `PATCH` | `/api/purchase-orders/{id}/cancel` | Cancel a draft or placed purchase order. | None | `PurchaseOrderResponse` | `200` | `400`, `404`, `500` |

DTO design:

- `PurchaseOrderCreateRequest`: `poNumber`, `supplierId`, `orderDate`, `expectedDeliveryDate`, `items`. Generated IDs, timestamps, status, and total amount are not accepted.
- `PurchaseOrderItemRequest`: `materialId`, `quantity`, `unitPrice`, `expectedDate`.
- `PurchaseOrderUpdateRequest`: `expectedDeliveryDate`. Supplier, PO number, items, status, and total amount are not changed through this request.
- `PurchaseOrderResponse`: `id`, `poNumber`, supplier summary, `status`, `orderDate`, `expectedDeliveryDate`, `actualDeliveryDate`, server-calculated `totalAmount`, item responses, `createdAt`, `updatedAt`.
- `PurchaseOrderItemResponse`: `id`, material summary, `quantity`, `unitPrice`, `expectedDate`, `receivedQuantity`, `status`, `lineAmount`.

Validation expectations:

- Request DTO: `poNumber` `@NotBlank`, `supplierId` and `materialId` `@Positive`, item list `@NotEmpty`, item `quantity` `@Positive`, item `unitPrice` `@PositiveOrZero`.
- Service: supplier exists, PO number is unique, every material exists, expected dates are not before order date, item expected dates are not after the PO expected delivery date, total amount is calculated from `quantity * unitPrice`.
- Purchase order creation with multiple items is transactional. If any item is invalid or references a missing material, no partial purchase order is left behind.

Purchase order lifecycle:

```text
DRAFT -> PLACED
DRAFT -> CANCELLED
PLACED -> CANCELLED
```

`PARTIALLY_RECEIVED` and `RECEIVED` are reserved for the future inventory receipt workflow. They are recognized as business statuses but are not exposed as direct arbitrary status mutation endpoints in this phase.

## Deliveries

| Method | URI | Purpose | Request | Response | Success | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/deliveries` | List deliveries. Optional filter: `purchaseOrderId`. | None | `List<DeliveryResponse>` | `200` | `404`, `500` |
| `GET` | `/api/deliveries/{id}` | Get delivery by ID. | None | `DeliveryResponse` | `200` | `404`, `500` |
| `GET` | `/api/purchase-orders/{purchaseOrderId}/deliveries` | List deliveries for a purchase order. | None | `List<DeliveryResponse>` | `200` | `404`, `500` |
| `POST` | `/api/purchase-orders/{purchaseOrderId}/deliveries` | Create a delivery for a purchase order. | `DeliveryCreateRequest` | `DeliveryResponse` | `201` | `400`, `404`, `500` |
| `PUT` | `/api/deliveries/{id}` | Update delivery tracking/date/detail fields. | `DeliveryUpdateRequest` | `DeliveryResponse` | `200` | `400`, `404`, `500` |
| `PATCH` | `/api/deliveries/{id}/dispatch` | Move pending delivery to dispatched. | None | `DeliveryResponse` | `200` | `400`, `404`, `500` |
| `PATCH` | `/api/deliveries/{id}/in-transit` | Move dispatched or delayed delivery to in transit. | None | `DeliveryResponse` | `200` | `400`, `404`, `500` |
| `PATCH` | `/api/deliveries/{id}/delay` | Mark dispatched or in-transit delivery as delayed. | None | `DeliveryResponse` | `200` | `400`, `404`, `500` |
| `PATCH` | `/api/deliveries/{id}/deliver` | Mark dispatched, in-transit, or delayed delivery as delivered. | None | `DeliveryResponse` | `200` | `400`, `404`, `500` |
| `PATCH` | `/api/deliveries/{id}/cancel` | Cancel a non-terminal delivery. | None | `DeliveryResponse` | `200` | `400`, `404`, `500` |

DTO design:

- `DeliveryCreateRequest`: `trackingNumber`, `dispatchDate`, `expectedArrivalDate`, `actualArrivalDate`, `delayDays`, `notes`. Status is not client-controlled on create and defaults to `PENDING`.
- `DeliveryUpdateRequest`: same mutable delivery details as create. Purchase order and status are not changed through this request.
- `DeliveryResponse`: `id`, purchase order summary, `trackingNumber`, `dispatchDate`, `expectedArrivalDate`, `actualArrivalDate`, `status`, `delayDays`, `notes`.

Validation expectations:

- Request DTO: `trackingNumber` and `notes` size limits, `delayDays` `@PositiveOrZero`.
- Service: purchase order exists, delay days cannot be negative, expected or actual arrival cannot be before dispatch.
- Tracking number is not treated as unique because the current database schema does not enforce uniqueness.

Delivery lifecycle:

```text
PENDING -> DISPATCHED
PENDING -> CANCELLED
DISPATCHED -> IN_TRANSIT
DISPATCHED -> DELAYED
DISPATCHED -> DELIVERED
DISPATCHED -> CANCELLED
IN_TRANSIT -> DELAYED
IN_TRANSIT -> DELIVERED
IN_TRANSIT -> CANCELLED
DELAYED -> IN_TRANSIT
DELAYED -> DELIVERED
DELAYED -> CANCELLED
```

`DELIVERED` and `CANCELLED` are terminal delivery states in this phase.

Inventory receipt limitation:

- Creating a delivery does not update inventory.
- Marking a delivery delivered does not update inventory yet.
- The current delivery table does not track per-material delivered quantities, so automatic receipt processing would risk double-counting. Inventory receipt should be implemented in a later refinement with explicit received quantities and idempotent receipt tracking.

## Production Orders

| Method | URI | Purpose | Request | Response | Success | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/production-orders` | List production orders. Optional filters: `productId`, `status`. | None | `List<ProductionOrderResponse>` | `200` | `400`, `404`, `500` |
| `GET` | `/api/production-orders/{id}` | Get production order by ID. | None | `ProductionOrderResponse` | `200` | `404`, `500` |
| `GET` | `/api/production-orders/by-number/{productionNumber}` | Get production order by number. | None | `ProductionOrderResponse` | `200` | `404`, `500` |
| `POST` | `/api/production-orders` | Create a production order for a product. | `ProductionOrderCreateRequest` | `ProductionOrderResponse` | `201` | `400`, `404`, `409`, `500` |
| `PUT` | `/api/production-orders/{id}` | Update production order planning fields. | `ProductionOrderUpdateRequest` | `ProductionOrderResponse` | `200` | `400`, `404`, `500` |
| `PUT` / `PATCH` | `/api/production-orders/{id}/status` | Transition production order lifecycle status. | `ProductionOrderStatusUpdateRequest` | `ProductionOrderResponse` | `200` | `400`, `404`, `500` |
| `POST` / `PATCH` | `/api/production-orders/{id}/cancel` | Cancel a production order. | None | `ProductionOrderResponse` | `200` | `400`, `404`, `500` |

DTO design:

- `ProductionOrderCreateRequest`: `productionNumber`, `productId`, `quantity`, `plannedStartDate`, `plannedEndDate`, `actualStartDate`, `actualEndDate`, `status`, `priority`, `createdBy`.
- `ProductionOrderUpdateRequest`: `quantity`, `plannedStartDate`, `plannedEndDate`, `actualStartDate`, `actualEndDate`, `priority`, `createdBy`.
- `ProductionOrderStatusUpdateRequest`: `status`.
- `ProductionOrderResponse`: `id`, `productionNumber`, `product` summary, `quantity`, `plannedStartDate`, `plannedEndDate`, `actualStartDate`, `actualEndDate`, `status`, `priority`, `createdBy` summary.

Validation expectations:

- Request DTO: `productionNumber` `@NotBlank`, `productId` `@NotNull`, `quantity` `@Positive`.
- Service: product exists, production number is unique, planned start date cannot be after planned end date, creator exists if specified.
- Production orders are not linked directly to customer orders in this phase; fulfillment allocation is deferred.

Production order lifecycle:

```text
PLANNED -> IN_PROGRESS -> COMPLETED
PLANNED -> CANCELLED
IN_PROGRESS -> CANCELLED
```

## Customer Orders

| Method | URI | Purpose | Request | Response | Success | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/customer-orders` | List customer orders. Optional filter: `status`. | None | `List<CustomerOrderResponse>` | `200` | `400`, `500` |
| `GET` | `/api/customer-orders/{id}` | Get customer order by ID. | None | `CustomerOrderResponse` | `200` | `404`, `500` |
| `GET` | `/api/customer-orders/by-number/{orderNumber}` | Get customer order by order number. | None | `CustomerOrderResponse` | `200` | `404`, `500` |
| `POST` | `/api/customer-orders` | Create customer order with one or more items. | `CustomerOrderCreateRequest` | `CustomerOrderResponse` | `201` | `400`, `404`, `409`, `500` |
| `PUT` | `/api/customer-orders/{id}` | Update customer order header fields. | `CustomerOrderUpdateRequest` | `CustomerOrderResponse` | `200` | `400`, `404`, `500` |
| `PUT` / `PATCH` | `/api/customer-orders/{id}/status` | Transition customer order lifecycle status. | `CustomerOrderStatusUpdateRequest` | `CustomerOrderResponse` | `200` | `400`, `404`, `500` |
| `POST` / `PATCH` | `/api/customer-orders/{id}/cancel` | Cancel a customer order. | None | `CustomerOrderResponse` | `200` | `400`, `404`, `500` |

DTO design:

- `CustomerOrderCreateRequest`: `orderNumber`, `customerName`, `orderDate`, `requiredDeliveryDate`, `status`, `priority`, `items`.
- `CustomerOrderItemRequest`: `productId`, `quantity`, `unitPrice`.
- `CustomerOrderUpdateRequest`: `customerName`, `orderDate`, `requiredDeliveryDate`, `priority`.
- `CustomerOrderStatusUpdateRequest`: `status`.
- `CustomerOrderResponse`: `id`, `orderNumber`, `customerName`, `orderDate`, `requiredDeliveryDate`, `status`, `priority`, server-calculated `totalAmount`, `items`.
- `CustomerOrderItemResponse`: `id`, `product` summary, `quantity`, `unitPrice`, `lineAmount`.

Validation expectations:

- Request DTO: `orderNumber` `@NotBlank`, `customerName` `@NotBlank`, `items` `@NotEmpty`, item `productId` `@NotNull`, item `quantity` `@Positive`, item `unitPrice` `@PositiveOrZero`.
- Service: order number is unique, every product exists, order date cannot be after required delivery date, total amount is calculated server-side from `sum(quantity * unitPrice)`.
- Customer order creation is atomic across all items.

Customer order lifecycle:

```text
PENDING -> CONFIRMED -> IN_PROGRESS -> FULFILLED
PENDING -> CANCELLED
CONFIRMED -> CANCELLED
IN_PROGRESS -> CANCELLED
```

Operational & Inventory boundaries:
- Creating or advancing production orders does NOT consume inventory or generate finished goods stock in this phase.
- Creating customer orders does NOT decrement inventory or reserve finished goods in this phase.
- Direct foreign keys between `ProductionOrder` and `CustomerOrder` are deferred until an explicit allocation/fulfillment model is introduced.

## Validation Strategy

DTO validation should handle structural request problems:

- Required text: `@NotBlank`
- Required IDs or values: `@NotNull`
- Positive quantities: `@Positive`
- Non-negative quantities, costs, durations, and scores: `@PositiveOrZero`
- Email syntax: `@Email`
- String limits: `@Size`

Service validation must continue to own business invariants:

- Duplicate supplier/material/product codes
- Duplicate supplier-material relationships
- Duplicate product BOM entries
- Duplicate material/warehouse inventory records
- Duplicate purchase order numbers
- Missing referenced supplier, material, or product
- Safe deletion when dependent operational records exist
- Negative stock prevention during inventory adjustment
- Purchase order and delivery lifecycle transitions
- Server-side purchase order total calculation
- Business-level percentage ranges

## Pagination, Sorting, And Filtering

The first controller implementation can return simple lists for SIH development speed, but the endpoint design should leave room for standard query parameters:

- `page`, `size`, `sort`
- `status`, `code`, `criticality`
- `supplierId`, `materialId`, `productId`
- `warehouseLocation`

Pagination will likely be needed for suppliers, materials, products, purchase orders, inventory, alerts, predictions, and historical outcomes as data grows.

## Future Compatibility

This operational API structure leaves room for later resource groups without changing the core paths:

- `/api/risk-assessments`
- `/api/predictions`
- `/api/impact-analyses`
- `/api/alternative-suppliers`
- `/api/recommendations`
- `/api/simulations`
- `/api/alerts`
- `/api/analytics`

Those resources should build on the same DTO, error response, validation, and relationship-summary conventions.

## Health Check

| Method | URI | Purpose | Request | Response | Success | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `GET` | `/api/health` | Confirms that the backend API application is running. | None | Health status object | `200` | `500` |

Response example:

```json
{
  "status": "UP",
  "service": "supply-chain-api"
}
```
