package com.sih.supplychain.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
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
class ProcurementApiIntegrationTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void purchaseOrderApiCreatesMultipleItemsCalculatesTotalAndSupportsFilters() throws Exception {
        String suffix = suffix();
        long supplierId = createSupplier(suffix);
        long steelId = createMaterial(suffix + "-ST", "Steel");
        long copperId = createMaterial(suffix + "-CU", "Copper");
        String poNumber = "API-PO-" + suffix;

        long purchaseOrderId = postJson("/api/purchase-orders", purchaseOrderJson(
                poNumber,
                supplierId,
                steelId,
                copperId
        ))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/purchase-orders/")))
                .andExpect(jsonPath("$.poNumber").value(poNumber))
                .andExpect(jsonPath("$.supplier.id").value(supplierId))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(211.25))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.supplier.purchaseOrders").doesNotExist())
                .andReturnId();

        this.mockMvc.perform(get("/api/purchase-orders/{id}", purchaseOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.poNumber").value(poNumber))
                .andExpect(jsonPath("$.items[0].material.id").isNumber());

        this.mockMvc.perform(get("/api/purchase-orders/by-number/{poNumber}", poNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(purchaseOrderId));
        this.mockMvc.perform(get("/api/purchase-orders").param("supplierId", Long.toString(supplierId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(purchaseOrderId));

        patchJson("/api/purchase-orders/" + purchaseOrderId + "/place")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"));
        patchJson("/api/purchase-orders/" + purchaseOrderId + "/place")
                .andExpect(status().isBadRequest());
    }

    @Test
    void purchaseOrderApiMapsBusinessErrorsAndRollsBackFailedCreate() throws Exception {
        String suffix = suffix();
        long supplierId = createSupplier(suffix);
        long materialId = createMaterial(suffix, "Steel");

        postJson("/api/purchase-orders", purchaseOrderJson("API-PO-" + suffix, 999999999L, materialId, materialId))
                .andExpect(status().isNotFound());

        String missingMaterialPo = "API-PO-MISS-" + suffix;
        postJson("/api/purchase-orders", purchaseOrderJson(missingMaterialPo, supplierId, materialId, 999999999L))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get("/api/purchase-orders/by-number/{poNumber}", missingMaterialPo))
                .andExpect(status().isNotFound());

        String validPoJson = purchaseOrderJson("API-PO-DUP-" + suffix, supplierId, materialId, materialId);
        postJson("/api/purchase-orders", validPoJson).andExpect(status().isCreated());
        postJson("/api/purchase-orders", validPoJson).andExpect(status().isConflict());

        postJson("/api/purchase-orders", """
                {
                  "poNumber": "API-PO-EMPTY-%s",
                  "supplierId": %d,
                  "expectedDeliveryDate": "%s",
                  "items": []
                }
                """.formatted(suffix, supplierId, LocalDate.now().plusDays(7)))
                .andExpect(status().isBadRequest());

        postJson("/api/purchase-orders", purchaseOrderJsonWithItem(
                "API-PO-BAD-QTY-" + suffix,
                supplierId,
                materialId,
                "0",
                "12.50"
        ))
                .andExpect(status().isBadRequest());

        postJson("/api/purchase-orders", purchaseOrderJsonWithItem(
                "API-PO-BAD-PRICE-" + suffix,
                supplierId,
                materialId,
                "5.000",
                "-1.00"
        ))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deliveryApiCreatesListsUpdatesAndTransitions() throws Exception {
        String suffix = suffix();
        long supplierId = createSupplier(suffix);
        long materialId = createMaterial(suffix, "Steel");
        long purchaseOrderId = createPurchaseOrder("API-PO-DEL-" + suffix, supplierId, materialId);

        long deliveryId = postJson("/api/purchase-orders/" + purchaseOrderId + "/deliveries", deliveryJson(suffix))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/deliveries/")))
                .andExpect(jsonPath("$.purchaseOrder.id").value(purchaseOrderId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturnId();

        this.mockMvc.perform(get("/api/deliveries/{id}", deliveryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value("TRK-" + suffix));
        this.mockMvc.perform(get("/api/purchase-orders/{purchaseOrderId}/deliveries", purchaseOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(deliveryId));

        putJson("/api/deliveries/" + deliveryId, """
                {
                  "trackingNumber": "TRK-UPD-%s",
                  "dispatchDate": "%s",
                  "expectedArrivalDate": "%s",
                  "delayDays": 1,
                  "notes": "Updated delivery"
                }
                """.formatted(suffix, LocalDate.now().plusDays(1), LocalDate.now().plusDays(4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value("TRK-UPD-" + suffix));

        patchJson("/api/deliveries/" + deliveryId + "/dispatch")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPATCHED"));
        patchJson("/api/deliveries/" + deliveryId + "/in-transit")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
        patchJson("/api/deliveries/" + deliveryId + "/deliver")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
        patchJson("/api/deliveries/" + deliveryId + "/cancel")
                .andExpect(status().isBadRequest());
    }

    @Test
    void deliveryApiMapsValidationAndMissingResourceErrors() throws Exception {
        String suffix = suffix();
        long supplierId = createSupplier(suffix);
        long materialId = createMaterial(suffix, "Steel");
        long purchaseOrderId = createPurchaseOrder("API-PO-ERR-DEL-" + suffix, supplierId, materialId);

        postJson("/api/purchase-orders/999999999/deliveries", deliveryJson(suffix))
                .andExpect(status().isNotFound());

        postJson("/api/purchase-orders/" + purchaseOrderId + "/deliveries", """
                {
                  "trackingNumber": "TRK-BAD-%s",
                  "dispatchDate": "%s",
                  "actualArrivalDate": "%s"
                }
                """.formatted(suffix, LocalDate.now().plusDays(3), LocalDate.now().plusDays(2)))
                .andExpect(status().isBadRequest());

        postJson("/api/purchase-orders/" + purchaseOrderId + "/deliveries", """
                {
                  "trackingNumber": "TRK-NEG-%s",
                  "delayDays": -1
                }
                """.formatted(suffix))
                .andExpect(status().isBadRequest());

        long deliveryId = postJson("/api/purchase-orders/" + purchaseOrderId + "/deliveries", deliveryJson(suffix))
                .andExpect(status().isCreated())
                .andReturnId();

        patchJson("/api/deliveries/" + deliveryId + "/deliver")
                .andExpect(status().isBadRequest());
    }

    private long createSupplier(String suffix) throws Exception {
        return postJson("/api/suppliers", """
                {
                  "name": "Procurement Supplier %s",
                  "code": "PROC-SUP-%s",
                  "email": "proc.supplier.%s@example.com",
                  "leadTimeDays": 5,
                  "capacity": 500.000,
                  "reliabilityScore": 95.00,
                  "status": "ACTIVE"
                }
                """.formatted(suffix, suffix, suffix))
                .andExpect(status().isCreated())
                .andReturnId();
    }

    private long createMaterial(String suffix, String name) throws Exception {
        return postJson("/api/materials", """
                {
                  "code": "PROC-MAT-%s",
                  "name": "%s %s",
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
                """.formatted(suffix, name, suffix))
                .andExpect(status().isCreated())
                .andReturnId();
    }

    private long createPurchaseOrder(String poNumber, long supplierId, long materialId) throws Exception {
        return postJson("/api/purchase-orders", purchaseOrderJson(poNumber, supplierId, materialId, materialId))
                .andExpect(status().isCreated())
                .andReturnId();
    }

    private String purchaseOrderJson(String poNumber, long supplierId, long firstMaterialId, long secondMaterialId) {
        return """
                {
                  "poNumber": "%s",
                  "supplierId": %d,
                  "orderDate": "%s",
                  "expectedDeliveryDate": "%s",
                  "items": [
                    {
                      "materialId": %d,
                      "quantity": 5.000,
                      "unitPrice": 12.50,
                      "expectedDate": "%s"
                    },
                    {
                      "materialId": %d,
                      "quantity": 2.000,
                      "unitPrice": 74.375,
                      "expectedDate": "%s"
                    }
                  ]
                }
                """.formatted(
                poNumber,
                supplierId,
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                firstMaterialId,
                LocalDate.now().plusDays(5),
                secondMaterialId,
                LocalDate.now().plusDays(6)
        );
    }

    private String purchaseOrderJsonWithItem(
            String poNumber,
            long supplierId,
            long materialId,
            String quantity,
            String unitPrice
    ) {
        return """
                {
                  "poNumber": "%s",
                  "supplierId": %d,
                  "orderDate": "%s",
                  "expectedDeliveryDate": "%s",
                  "items": [
                    {
                      "materialId": %d,
                      "quantity": %s,
                      "unitPrice": %s,
                      "expectedDate": "%s"
                    }
                  ]
                }
                """.formatted(
                poNumber,
                supplierId,
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                materialId,
                quantity,
                unitPrice,
                LocalDate.now().plusDays(5)
        );
    }

    private String deliveryJson(String suffix) {
        return """
                {
                  "trackingNumber": "TRK-%s",
                  "dispatchDate": "%s",
                  "expectedArrivalDate": "%s",
                  "delayDays": 0,
                  "notes": "Phase 6A delivery"
                }
                """.formatted(suffix, LocalDate.now().plusDays(1), LocalDate.now().plusDays(4));
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

    private MockMvcResult patchJson(String uri) throws Exception {
        return new MockMvcResult(this.mockMvc.perform(patch(uri)));
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
