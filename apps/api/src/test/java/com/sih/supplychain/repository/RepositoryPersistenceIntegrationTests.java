package com.sih.supplychain.repository;

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
import com.sih.supplychain.domain.Role;
import com.sih.supplychain.domain.Supplier;
import com.sih.supplychain.domain.SupplierMaterial;
import com.sih.supplychain.domain.SupplierPerformance;
import com.sih.supplychain.domain.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_HOST", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PORT", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class RepositoryPersistenceIntegrationTests {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private SupplierMaterialRepository supplierMaterialRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMaterialRepository productMaterialRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private SupplierPerformanceRepository supplierPerformanceRepository;

    @Autowired
    private ProductionOrderRepository productionOrderRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private CustomerOrderItemRepository customerOrderItemRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void repositoriesPersistAndQueryRepresentativeOperationalRelationships() {
        String suffix = Long.toString(System.nanoTime());

        Role role = this.roleRepository.save(new Role("PHASE4A_TEST_" + suffix));
        User user = this.userRepository.save(new User(
                "Phase 4A User " + suffix,
                "phase4a." + suffix + "@example.com",
                role
        ));

        Supplier supplier = new Supplier("Phase 4A Supplier " + suffix, "SUP-" + suffix);
        supplier.setStatus("ACTIVE");
        supplier = this.supplierRepository.save(supplier);

        Material material = new Material("MAT-" + suffix, "Phase 4A Material " + suffix);
        material.setStatus("ACTIVE");
        material.setCriticality("HIGH");
        material = this.materialRepository.save(material);

        flushAndClear();

        Role persistedRole = this.roleRepository.findByName(role.getName()).orElseThrow();
        User persistedUser = this.userRepository.findByEmail(user.getEmail()).orElseThrow();
        Supplier persistedSupplier = this.supplierRepository.findByCode(supplier.getCode()).orElseThrow();
        Material persistedMaterial = this.materialRepository.findByCode(material.getCode()).orElseThrow();

        assertThat(this.userRepository.findByRoleId(persistedRole.getId()))
                .extracting(User::getId)
                .contains(persistedUser.getId());
        assertThat(this.userRepository.findByActive(true))
                .extracting(User::getId)
                .contains(persistedUser.getId());
        assertThat(this.supplierRepository.findByStatus("ACTIVE"))
                .extracting(Supplier::getId)
                .contains(persistedSupplier.getId());
        assertThat(this.materialRepository.findByStatus("ACTIVE"))
                .extracting(Material::getId)
                .contains(persistedMaterial.getId());
        assertThat(this.materialRepository.findByCriticality("HIGH"))
                .extracting(Material::getId)
                .contains(persistedMaterial.getId());

        SupplierMaterial supplierMaterial = this.supplierMaterialRepository.save(
                new SupplierMaterial(persistedSupplier, persistedMaterial)
        );

        flushAndClear();

        SupplierMaterial persistedSupplierMaterial = this.supplierMaterialRepository
                .findBySupplierIdAndMaterialId(persistedSupplier.getId(), persistedMaterial.getId())
                .orElseThrow();
        assertThat(persistedSupplierMaterial.getId()).isEqualTo(supplierMaterial.getId());
        assertThat(this.supplierMaterialRepository.findBySupplierId(persistedSupplier.getId()))
                .extracting(SupplierMaterial::getId)
                .contains(supplierMaterial.getId());
        assertThat(this.supplierMaterialRepository.findByMaterialId(persistedMaterial.getId()))
                .extracting(SupplierMaterial::getId)
                .contains(supplierMaterial.getId());

        Product product = this.productRepository.save(new Product("PRD-" + suffix, "Phase 4A Product " + suffix));
        ProductMaterial productMaterial = this.productMaterialRepository.save(
                new ProductMaterial(product, persistedMaterial, new BigDecimal("2.500"))
        );

        Inventory inventory = new Inventory(persistedMaterial, "MAIN-WH-" + suffix);
        inventory.setQuantityOnHand(new BigDecimal("100.000"));
        inventory = this.inventoryRepository.save(inventory);

        flushAndClear();

        Product persistedProduct = this.productRepository.findByCode(product.getCode()).orElseThrow();
        assertThat(this.productMaterialRepository.findByProductId(persistedProduct.getId()))
                .extracting(ProductMaterial::getId)
                .contains(productMaterial.getId());
        assertThat(this.productMaterialRepository.findByMaterialId(persistedMaterial.getId()))
                .extracting(ProductMaterial::getId)
                .contains(productMaterial.getId());

        Inventory persistedInventory = this.inventoryRepository
                .findByMaterialIdAndWarehouseLocation(persistedMaterial.getId(), inventory.getWarehouseLocation())
                .orElseThrow();
        assertThat(persistedInventory.getQuantityOnHand()).isEqualByComparingTo("100.000");
        assertThat(this.inventoryRepository.findByMaterialId(persistedMaterial.getId()))
                .extracting(Inventory::getId)
                .contains(inventory.getId());
        assertThat(this.inventoryRepository.findByWarehouseLocation(inventory.getWarehouseLocation()))
                .extracting(Inventory::getId)
                .contains(inventory.getId());

        PurchaseOrder purchaseOrder = new PurchaseOrder("PO-" + suffix, persistedSupplier);
        purchaseOrder.setStatus("OPEN");
        purchaseOrder = this.purchaseOrderRepository.save(purchaseOrder);
        PurchaseOrderItem purchaseOrderItem = this.purchaseOrderItemRepository.save(
                new PurchaseOrderItem(purchaseOrder, persistedMaterial, new BigDecimal("15.000"))
        );
        Delivery delivery = this.deliveryRepository.save(new Delivery(purchaseOrder, "TRK-" + suffix));

        flushAndClear();

        PurchaseOrder persistedPurchaseOrder = this.purchaseOrderRepository.findByPoNumber(purchaseOrder.getPoNumber())
                .orElseThrow();
        assertThat(this.purchaseOrderRepository.findBySupplierId(persistedSupplier.getId()))
                .extracting(PurchaseOrder::getId)
                .contains(purchaseOrder.getId());
        assertThat(this.purchaseOrderRepository.findByStatus("OPEN"))
                .extracting(PurchaseOrder::getId)
                .contains(purchaseOrder.getId());
        assertThat(this.purchaseOrderItemRepository.findByPurchaseOrderId(persistedPurchaseOrder.getId()))
                .extracting(PurchaseOrderItem::getId)
                .contains(purchaseOrderItem.getId());
        assertThat(this.purchaseOrderItemRepository.findByMaterialId(persistedMaterial.getId()))
                .extracting(PurchaseOrderItem::getId)
                .contains(purchaseOrderItem.getId());
        assertThat(persistedPurchaseOrder.getItems())
                .extracting(PurchaseOrderItem::getId)
                .contains(purchaseOrderItem.getId());
        assertThat(this.deliveryRepository.findByPurchaseOrderId(persistedPurchaseOrder.getId()))
                .extracting(Delivery::getId)
                .contains(delivery.getId());
        assertThat(this.deliveryRepository.findByTrackingNumber(delivery.getTrackingNumber()))
                .extracting(Delivery::getId)
                .contains(delivery.getId());

        SupplierPerformance olderPerformance = new SupplierPerformance(
                persistedSupplier,
                LocalDate.of(2026, 1, 1)
        );
        olderPerformance.setOverallScore(new BigDecimal("78.00"));
        SupplierPerformance latestPerformance = new SupplierPerformance(
                persistedSupplier,
                LocalDate.of(2026, 2, 1)
        );
        latestPerformance.setOverallScore(new BigDecimal("91.00"));
        this.supplierPerformanceRepository.saveAll(List.of(olderPerformance, latestPerformance));

        ProductionOrder productionOrder = new ProductionOrder(
                "PROD-" + suffix,
                persistedProduct,
                new BigDecimal("20.000")
        );
        productionOrder.setStatus("PLANNED");
        productionOrder = this.productionOrderRepository.save(productionOrder);

        CustomerOrder customerOrder = new CustomerOrder("CO-" + suffix, "Phase 4A Customer " + suffix);
        customerOrder.setStatus("OPEN");
        customerOrder = this.customerOrderRepository.save(customerOrder);
        CustomerOrderItem customerOrderItem = this.customerOrderItemRepository.save(
                new CustomerOrderItem(customerOrder, persistedProduct, new BigDecimal("4.000"))
        );

        flushAndClear();

        assertThat(this.supplierPerformanceRepository.findBySupplierId(persistedSupplier.getId()))
                .extracting(SupplierPerformance::getId)
                .contains(olderPerformance.getId(), latestPerformance.getId());
        assertThat(this.supplierPerformanceRepository
                .findBySupplierIdOrderByEvaluationDateDesc(persistedSupplier.getId()))
                .first()
                .extracting(SupplierPerformance::getId)
                .isEqualTo(latestPerformance.getId());

        ProductionOrder persistedProductionOrder = this.productionOrderRepository
                .findByProductionNumber(productionOrder.getProductionNumber())
                .orElseThrow();
        assertThat(this.productionOrderRepository.findByProductId(persistedProduct.getId()))
                .extracting(ProductionOrder::getId)
                .contains(productionOrder.getId());
        assertThat(this.productionOrderRepository.findByStatus("PLANNED"))
                .extracting(ProductionOrder::getId)
                .contains(persistedProductionOrder.getId());

        CustomerOrder persistedCustomerOrder = this.customerOrderRepository
                .findByOrderNumber(customerOrder.getOrderNumber())
                .orElseThrow();
        assertThat(this.customerOrderRepository.findByStatus("OPEN"))
                .extracting(CustomerOrder::getId)
                .contains(customerOrder.getId());
        assertThat(this.customerOrderItemRepository.findByCustomerOrderId(persistedCustomerOrder.getId()))
                .extracting(CustomerOrderItem::getId)
                .contains(customerOrderItem.getId());
        assertThat(this.customerOrderItemRepository.findByProductId(persistedProduct.getId()))
                .extracting(CustomerOrderItem::getId)
                .contains(customerOrderItem.getId());
        assertThat(persistedCustomerOrder.getItems())
                .extracting(CustomerOrderItem::getId)
                .contains(customerOrderItem.getId());
    }

    private void flushAndClear() {
        this.entityManager.flush();
        this.entityManager.clear();
    }
}
