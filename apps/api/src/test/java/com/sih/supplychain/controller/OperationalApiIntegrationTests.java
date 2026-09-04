package com.sih.supplychain.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.startsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_HOST", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PORT", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class OperationalApiIntegrationTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void supplierApiCreatesReadsUpdatesDeletesAndMapsErrors() throws Exception {
        String suffix = suffix();
        String createJson = supplierJson(suffix);

        long supplierId = postJson("/api/suppliers", createJson)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/suppliers/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").value("API-SUP-" + suffix))
                .andExpect(jsonPath("$.supplierMaterials").doesNotExist())
                .andReturnId();

        this.mockMvc.perform(get("/api/suppliers/{id}", supplierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("API-SUP-" + suffix));

        this.mockMvc.perform(get("/api/suppliers").param("code", "API-SUP-" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(supplierId));

        postJson("/api/suppliers", createJson)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/suppliers"));

        postJson("/api/suppliers", """
                {
                  "name": "",
                  "code": "BAD-SUP-%s",
                  "email": "not-an-email",
                  "leadTimeDays": -1
                }
                """.formatted(suffix))
                .andExpect(status().isBadRequest());

        this.mockMvc.perform(get("/api/suppliers/{id}", 999999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        putJson("/api/suppliers/" + supplierId, """
                {
                  "name": "Updated Supplier %s",
                  "code": "API-SUP-%s",
                  "contactPerson": "Procurement",
                  "email": "updated.supplier.%s@example.com",
                  "leadTimeDays": 3,
                  "capacity": 250.000,
                  "reliabilityScore": 96.00,
                  "status": "ACTIVE"
                }
                """.formatted(suffix, suffix, suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Supplier " + suffix));

        this.mockMvc.perform(delete("/api/suppliers/{id}", supplierId))
                .andExpect(status().isNoContent());
    }

    @Test
    void materialApiCreatesReadsAndMapsValidationErrors() throws Exception {
        String suffix = suffix();
        String createJson = materialJson(suffix);

        long materialId = postJson("/api/materials", createJson)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("API-MAT-" + suffix))
                .andReturnId();

        this.mockMvc.perform(get("/api/materials/{id}", materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("API-MAT-" + suffix));

        postJson("/api/materials", createJson)
                .andExpect(status().isConflict());

        postJson("/api/materials", """
                {
                  "code": "",
                  "name": "",
                  "unitCost": -1.00
                }
                """)
                .andExpect(status().isBadRequest());

        this.mockMvc.perform(get("/api/materials/{id}", 999999999L))
                .andExpect(status().isNotFound());

        putJson("/api/materials/" + materialId, """
                {
                  "code": "API-MAT-%s",
                  "name": "Updated Material %s",
                  "unit": "KG",
                  "unitCost": 12.50,
                  "criticality": "HIGH",
                  "currentStock": 100.000,
                  "status": "ACTIVE"
                }
                """.formatted(suffix, suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Material " + suffix));
    }

    @Test
    void supplierMaterialApiCreatesRelationshipsAndMapsErrors() throws Exception {
        String suffix = suffix();
        long supplierId = createSupplier(suffix);
        long materialId = createMaterial(suffix);
        String relationshipJson = supplierMaterialJson();

        long relationshipId = postJson("/api/suppliers/" + supplierId + "/materials/" + materialId, relationshipJson)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplier.id").value(supplierId))
                .andExpect(jsonPath("$.material.id").value(materialId))
                .andExpect(jsonPath("$.supplier.supplierMaterials").doesNotExist())
                .andReturnId();

        postJson("/api/suppliers/" + supplierId + "/materials/" + materialId, relationshipJson)
                .andExpect(status().isConflict());
        postJson("/api/suppliers/999999999/materials/" + materialId, relationshipJson)
                .andExpect(status().isNotFound());
        postJson("/api/suppliers/" + supplierId + "/materials/999999999", relationshipJson)
                .andExpect(status().isNotFound());

        this.mockMvc.perform(get("/api/supplier-materials/{id}", relationshipId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(relationshipId));
        this.mockMvc.perform(get("/api/suppliers/{supplierId}/materials", supplierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(relationshipId));
        this.mockMvc.perform(get("/api/materials/{materialId}/suppliers", materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(relationshipId));
    }

    @Test
    void productAndBomApisCreateRelationshipsAndMapErrors() throws Exception {
        String suffix = suffix();
        long productId = createProduct(suffix);
        long materialId = createMaterial(suffix);
        String bomJson = """
                {
                  "quantityRequired": 2.500,
                  "unit": "KG",
                  "wastagePercentage": 5.00
                }
                """;

        long bomId = postJson("/api/products/" + productId + "/bom/materials/" + materialId, bomJson)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.product.id").value(productId))
                .andExpect(jsonPath("$.material.id").value(materialId))
                .andReturnId();

        postJson("/api/products/" + productId + "/bom/materials/" + materialId, bomJson)
                .andExpect(status().isConflict());

        postJson("/api/products/" + productId + "/bom/materials/" + materialId, """
                {
                  "quantityRequired": 0,
                  "unit": "KG"
                }
                """)
                .andExpect(status().isBadRequest());

        this.mockMvc.perform(get("/api/products/{productId}/bom", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bomId));
        this.mockMvc.perform(get("/api/materials/{materialId}/products", materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bomId));
    }

    @Test
    void productApiCreatesAndMapsDuplicateErrors() throws Exception {
        String suffix = suffix();
        String productJson = productJson(suffix);

        long productId = postJson("/api/products", productJson)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("API-PRD-" + suffix))
                .andReturnId();

        this.mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("API-PRD-" + suffix));

        postJson("/api/products", productJson)
                .andExpect(status().isConflict());
    }

    @Test
    void inventoryApiCreatesRetrievesAdjustsAndMapsErrors() throws Exception {
        String suffix = suffix();
        long materialId = createMaterial(suffix);
        String inventoryJson = inventoryJson(materialId, suffix, "100.000");

        long inventoryId = postJson("/api/inventory", inventoryJson)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.material.id").value(materialId))
                .andExpect(jsonPath("$.warehouseLocation").value("API-WH-" + suffix))
                .andReturnId();

        this.mockMvc.perform(get("/api/inventory/{id}", inventoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inventoryId));
        this.mockMvc.perform(get("/api/materials/{materialId}/inventory", materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(inventoryId));
        this.mockMvc.perform(get("/api/materials/{materialId}/inventory/{warehouseLocation}",
                        materialId, "API-WH-" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inventoryId));

        postJson("/api/inventory", inventoryJson)
                .andExpect(status().isConflict());
        postJson("/api/inventory", inventoryJson(materialId, "NEG-" + suffix, "-1.000"))
                .andExpect(status().isBadRequest());

        patchJson("/api/inventory/" + inventoryId + "/adjust", """
                {
                  "quantityChange": -20.000
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(80.000));

        patchJson("/api/inventory/" + inventoryId + "/adjust", """
                {
                  "quantityChange": -100.000
                }
                """)
                .andExpect(status().isBadRequest());
    }

    private long createSupplier(String suffix) throws Exception {
        return postJson("/api/suppliers", supplierJson(suffix))
                .andExpect(status().isCreated())
                .andReturnId();
    }

    private long createMaterial(String suffix) throws Exception {
        return postJson("/api/materials", materialJson(suffix))
                .andExpect(status().isCreated())
                .andReturnId();
    }

    private long createProduct(String suffix) throws Exception {
        return postJson("/api/products", productJson(suffix))
                .andExpect(status().isCreated())
                .andReturnId();
    }

    private String supplierJson(String suffix) {
        return """
                {
                  "name": "API Supplier %s",
                  "code": "API-SUP-%s",
                  "contactPerson": "Procurement",
                  "email": "supplier.%s@example.com",
                  "leadTimeDays": 5,
                  "capacity": 500.000,
                  "reliabilityScore": 95.00,
                  "status": "ACTIVE"
                }
                """.formatted(suffix, suffix, suffix);
    }

    private String materialJson(String suffix) {
        return """
                {
                  "code": "API-MAT-%s",
                  "name": "API Material %s",
                  "category": "Raw",
                  "unit": "KG",
                  "unitCost": 10.00,
                  "criticality": "HIGH",
                  "currentStock": 100.000,
                  "safetyStock": 10.000,
                  "reorderPoint": 20.000,
                  "dailyConsumption": 5.000,
                  "status": "ACTIVE"
                }
                """.formatted(suffix, suffix);
    }

    private String productJson(String suffix) {
        return """
                {
                  "code": "API-PRD-%s",
                  "name": "API Product %s",
                  "category": "Assembly",
                  "unitCost": 50.00,
                  "sellingPrice": 75.00,
                  "productionTimeHours": 4.00,
                  "status": "ACTIVE"
                }
                """.formatted(suffix, suffix);
    }

    private String supplierMaterialJson() {
        return """
                {
                  "unitPrice": 8.25,
                  "leadTimeDays": 4,
                  "minimumOrderQuantity": 10.000,
                  "maximumCapacity": 1000.000,
                  "reliabilityScore": 94.00,
                  "status": "ACTIVE"
                }
                """;
    }

    private String inventoryJson(long materialId, String suffix, String quantityOnHand) {
        return """
                {
                  "materialId": %d,
                  "warehouseLocation": "API-WH-%s",
                  "quantityOnHand": %s,
                  "quantityReserved": 0,
                  "quantityIncoming": 0,
                  "safetyStock": 10.000,
                  "reorderPoint": 20.000
                }
                """.formatted(materialId, suffix, quantityOnHand);
    }

    private MockMvcResult postJson(String uri, String body) throws Exception {
        return new MockMvcResult(this.mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)));
    }

    private MockMvcResult putJson(String uri, String body) throws Exception {
        return new MockMvcResult(this.mockMvc.perform(put(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)));
    }

    private MockMvcResult patchJson(String uri, String body) throws Exception {
        return new MockMvcResult(this.mockMvc.perform(patch(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)));
    }

    private String suffix() {
        return Long.toString(System.nanoTime());
    }

    private final class MockMvcResult {

        private final org.springframework.test.web.servlet.ResultActions actions;

        private MockMvcResult(org.springframework.test.web.servlet.ResultActions actions) {
            this.actions = actions;
        }

        private MockMvcResult andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            this.actions.andExpect(matcher);
            return this;
        }

        private long andReturnId() throws Exception {
            String content = this.actions.andReturn().getResponse().getContentAsString();
            assertThat(content).isNotBlank();
            Matcher matcher = ID_PATTERN.matcher(content);
            assertThat(matcher.find()).isTrue();
            return Long.parseLong(matcher.group(1));
        }
    }
}
