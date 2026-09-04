# Domain Model

The operational domain model is implemented as JPA entities, PostgreSQL Flyway migrations, and Spring Data JPA repositories.

## Implemented Tables And Entities

- `roles` / `Role`
- `users` / `User`
- `suppliers` / `Supplier`
- `materials` / `Material`
- `supplier_materials` / `SupplierMaterial`
- `products` / `Product`
- `product_materials` / `ProductMaterial`
- `inventories` / `Inventory`
- `purchase_orders` / `PurchaseOrder`
- `purchase_order_items` / `PurchaseOrderItem`
- `deliveries` / `Delivery`
- `supplier_performances` / `SupplierPerformance`
- `production_orders` / `ProductionOrder`
- `customer_orders` / `CustomerOrder`
- `customer_order_items` / `CustomerOrderItem`

## Implemented Relationships

- `Role` 1 -> N `User`
- `Supplier` 1 -> N `SupplierMaterial`
- `Material` 1 -> N `SupplierMaterial`
- `Product` 1 -> N `ProductMaterial`
- `Material` 1 -> N `ProductMaterial`
- `Material` 1 -> N `Inventory`
- `Supplier` 1 -> N `PurchaseOrder`
- `User` 1 -> N `PurchaseOrder`
- `PurchaseOrder` 1 -> N `PurchaseOrderItem`
- `Material` 1 -> N `PurchaseOrderItem`
- `PurchaseOrder` 1 -> N `Delivery`
- `Supplier` 1 -> N `SupplierPerformance`
- `Product` 1 -> N `ProductionOrder`
- `User` 1 -> N `ProductionOrder`
- `CustomerOrder` 1 -> N `CustomerOrderItem`
- `Product` 1 -> N `CustomerOrderItem`

## Constraints

- Unique role names, user emails, supplier codes, material codes, product codes, purchase order numbers, production order numbers, and customer order numbers
- Unique supplier/material pairs in `supplier_materials`
- Unique product/material pairs in `product_materials`
- Unique material/warehouse pairs in `inventories`
- Unique supplier/evaluation date pairs in `supplier_performances`
- Foreign keys for all implemented relationships

## Deliberate Choices

- The Java entity is named `User`, but the database table is named `users` to avoid PostgreSQL reserved-word friction.
- Status, priority, and criticality fields are stored as strings in this phase. Allowed values will be formalized when business workflows are implemented.
- Repository interfaces use Spring Data JPA derived queries for simple lookup paths. Business rules stay out of repositories.
- No prediction, risk scoring, recommendation, simulation, alert, authentication, Firebase, or ML entities are implemented yet.
