package com.sih.supplychain.domain;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@EnabledIfEnvironmentVariable(named = "DB_HOST", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PORT", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class PostgresPersistenceIntegrationTests {

    private static final List<String> EXPECTED_TABLES = List.of(
            "roles",
            "users",
            "suppliers",
            "materials",
            "supplier_materials",
            "products",
            "product_materials",
            "inventories",
            "purchase_orders",
            "purchase_order_items",
            "deliveries",
            "supplier_performances",
            "production_orders",
            "customer_orders",
            "customer_order_items"
    );

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    void postgresConnectionIsAvailable() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(2)).isTrue();
        }
    }

    @Test
    void flywayMigrationCreatedOperationalTablesAndSeededRoles() {
        List<String> tableNames = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                """, String.class);

        assertThat(tableNames).containsAll(EXPECTED_TABLES);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");

        Integer roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles", Integer.class);
        assertThat(roleCount).isNotNull().isGreaterThanOrEqualTo(3);
    }
}
