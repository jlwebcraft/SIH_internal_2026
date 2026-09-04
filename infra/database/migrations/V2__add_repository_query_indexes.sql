CREATE INDEX idx_suppliers_status ON suppliers (status);
CREATE INDEX idx_materials_status ON materials (status);
CREATE INDEX idx_materials_criticality ON materials (criticality);
CREATE INDEX idx_inventories_warehouse_location ON inventories (warehouse_location);
CREATE INDEX idx_purchase_orders_status ON purchase_orders (status);
CREATE INDEX idx_deliveries_tracking_number ON deliveries (tracking_number);
CREATE INDEX idx_supplier_performances_supplier_evaluation_date
    ON supplier_performances (supplier_id, evaluation_date DESC);
CREATE INDEX idx_production_orders_status ON production_orders (status);
CREATE INDEX idx_customer_orders_status ON customer_orders (status);
