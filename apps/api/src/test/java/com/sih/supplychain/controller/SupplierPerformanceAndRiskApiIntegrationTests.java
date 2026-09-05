package com.sih.supplychain.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class SupplierPerformanceAndRiskApiIntegrationTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void performanceAndRiskApi_evaluatesSupplierAndPersistsSnapshots() throws Exception {
        String suffix = suffix();
        long supplierId = createSupplier(suffix);
        long materialId = createMaterial(suffix);

        // Create PO
        long poId = postJson("/api/purchase-orders", """
                {
                  "poNumber": "PO-RISK-%s",
                  "supplierId": %d,
                  "orderDate": "%s",
                  "expectedDeliveryDate": "%s",
                  "items": [
                    {
                      "materialId": %d,
                      "quantity": 100,
                      "unitPrice": 45.00
                    }
                  ]
                }
                """.formatted(suffix, supplierId, LocalDate.now().minusDays(10), LocalDate.now().minusDays(3), materialId))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .transform(this::extractId);

        // Create Delivery with 2 days delay (expected: 3 days ago, actual: 1 day ago)
        long deliveryId = postJson("/api/purchase-orders/" + poId + "/deliveries", """
                {
                  "trackingNumber": "TRK-RISK-%s",
                  "dispatchDate": "%s",
                  "expectedArrivalDate": "%s",
                  "actualArrivalDate": "%s",
                  "delayDays": 2,
                  "notes": "Arrived late"
                }
                """.formatted(suffix, LocalDate.now().minusDays(8), LocalDate.now().minusDays(3), LocalDate.now().minusDays(1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .transform(this::extractId);

        // Dispatch and Mark Delivery DELIVERED
        this.mockMvc.perform(patch("/api/deliveries/" + deliveryId + "/dispatch"))
                .andExpect(status().isOk());
        this.mockMvc.perform(patch("/api/deliveries/" + deliveryId + "/deliver"))
                .andExpect(status().isOk());

        // 1. GET Performance
        this.mockMvc.perform(get("/api/suppliers/" + supplierId + "/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplier.id").value(supplierId))
                .andExpect(jsonPath("$.windowDays").value(90))
                .andExpect(jsonPath("$.completedDeliveries").value(1))
                .andExpect(jsonPath("$.onTimeDeliveryRate").value(0.0))
                .andExpect(jsonPath("$.averageDelayDays").value(2.0))
                .andExpect(jsonPath("$.insufficientHistory").value(false));

        // 2. GET Risk
        this.mockMvc.perform(get("/api/suppliers/" + supplierId + "/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplier.id").value(supplierId))
                .andExpect(jsonPath("$.overallScore").isNumber())
                .andExpect(jsonPath("$.riskLevel").isString())
                .andExpect(jsonPath("$.dimensionScores.deliveryRisk").isNumber())
                .andExpect(jsonPath("$.effectiveWeights.deliveryWeight").isNumber())
                .andExpect(jsonPath("$.topRiskDrivers").isArray());

        // 3. POST Snapshot
        this.mockMvc.perform(post("/api/suppliers/" + supplierId + "/performance/snapshot"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.onTimeDeliveryRate").value(0.0))
                .andExpect(jsonPath("$.averageDelayDays").value(2.0))
                .andExpect(jsonPath("$.overallScore").isNumber());

        // 4. GET History
        this.mockMvc.perform(get("/api/suppliers/" + supplierId + "/performance/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].supplier.id").value(supplierId));
    }

    @Test
    void zeroHistorySupplier_returnsInsufficientHistory() throws Exception {
        String suffix = suffix();
        long supplierId = createSupplier(suffix);

        this.mockMvc.perform(get("/api/suppliers/" + supplierId + "/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insufficientHistory").value(true))
                .andExpect(jsonPath("$.completedDeliveries").value(0));

        this.mockMvc.perform(get("/api/suppliers/" + supplierId + "/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insufficientHistory").value(true))
                .andExpect(jsonPath("$.topRiskDrivers[0]").value(org.hamcrest.Matchers.containsString("Insufficient historical")));
    }

    @Test
    void nonExistentSupplier_returns404() throws Exception {
        this.mockMvc.perform(get("/api/suppliers/999999/performance"))
                .andExpect(status().isNotFound());

        this.mockMvc.perform(get("/api/suppliers/999999/risk"))
                .andExpect(status().isNotFound());
    }

    private long createSupplier(String suffix) throws Exception {
        String json = """
                {
                  "name": "Supplier %s",
                  "code": "SUP-R-%s",
                  "leadTimeDays": 5,
                  "capacity": 5000,
                  "reliabilityScore": 85.00,
                  "status": "ACTIVE"
                }
                """.formatted(suffix, suffix);

        return postJson("/api/suppliers", json)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .transform(this::extractId);
    }

    private long createMaterial(String suffix) throws Exception {
        String json = """
                {
                  "code": "MAT-R-%s",
                  "name": "Material %s",
                  "category": "Raw Material",
                  "unit": "kg",
                  "unitCost": 45.00,
                  "criticality": "HIGH",
                  "status": "ACTIVE"
                }
                """.formatted(suffix, suffix);

        return postJson("/api/materials", json)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .transform(this::extractId);
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return this.mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private long extractId(String responseBody) {
        Matcher matcher = ID_PATTERN.matcher(responseBody);
        assertThat(matcher.find()).isTrue();
        return Long.parseLong(matcher.group(1));
    }

    private String suffix() {
        return String.valueOf(System.nanoTime());
    }
}
