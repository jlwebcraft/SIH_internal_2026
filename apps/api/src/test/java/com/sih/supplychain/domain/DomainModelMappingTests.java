package com.sih.supplychain.domain;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/supply_chain_db",
        "spring.datasource.username=test_user",
        "spring.datasource.password=test_password",
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.initialization-fail-timeout=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
class DomainModelMappingTests {

    private static final Set<String> EXPECTED_ENTITIES = Set.of(
            "Role",
            "User",
            "Supplier",
            "Material",
            "SupplierMaterial",
            "Product",
            "ProductMaterial",
            "Inventory",
            "PurchaseOrder",
            "PurchaseOrderItem",
            "Delivery",
            "SupplierPerformance",
            "ProductionOrder",
            "CustomerOrder",
            "CustomerOrderItem"
    );

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void allApprovedDomainEntitiesAreMapped() {
        Set<String> entityNames = this.entityManagerFactory.getMetamodel()
                .getEntities()
                .stream()
                .map(EntityType::getName)
                .collect(Collectors.toSet());

        assertThat(entityNames).containsAll(EXPECTED_ENTITIES);
    }
}
