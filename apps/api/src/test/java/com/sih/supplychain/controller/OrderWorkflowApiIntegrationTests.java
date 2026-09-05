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
class OrderWorkflowApiIntegrationTests {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void productionOrderWorkflow_createsRetrievesUpdatesAndTransitions() throws Exception {
        String suffix = suffix();
        long productId = createProduct(suffix);
        String prodNumber = "PR-API-" + suffix;

        String createJson = """
                {
                  "productionNumber": "%s",
                  "productId": %d,
                  "quantity": 100,
                  "plannedStartDate": "%s",
                  "plannedEndDate": "%s",
                  "priority": "HIGH"
                }
                """.formatted(
                prodNumber,
                productId,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(10)
        );

        long productionOrderId = postJson("/api/production-orders", createJson)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/production-orders/")))
                .andExpect(jsonPath("$.productionNumber").value(prodNumber))
                .andExpect(jsonPath("$.product.id").value(productId))
                .andExpect(jsonPath("$.quantity").value(100))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .transform(this::extractId);

        // Retrieve by ID
        this.mockMvc.perform(get("/api/production-orders/" + productionOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productionOrderId))
                .andExpect(jsonPath("$.productionNumber").value(prodNumber));

        // Retrieve by Number
        this.mockMvc.perform(get("/api/production-orders/by-number/" + prodNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productionOrderId));

        // List by product
        this.mockMvc.perform(get("/api/production-orders").param("productId", String.valueOf(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(productionOrderId));

        // Transition: PLANNED -> IN_PROGRESS
        this.mockMvc.perform(patch("/api/production-orders/" + productionOrderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.actualStartDate").isNotEmpty());

        // Transition: IN_PROGRESS -> COMPLETED
        this.mockMvc.perform(patch("/api/production-orders/" + productionOrderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.actualEndDate").isNotEmpty());

        // Invalid transition: COMPLETED -> PLANNED (should fail 400)
        this.mockMvc.perform(patch("/api/production-orders/" + productionOrderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PLANNED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("Invalid production order transition")));
    }

    @Test
    void customerOrderWorkflow_createsWithMultipleItemsCalculatesTotalAndSupportsLifecycle() throws Exception {
        String suffix = suffix();
        long productAId = createProduct(suffix + "-A");
        long productBId = createProduct(suffix + "-B");
        String orderNumber = "CO-API-" + suffix;

        String createJson = """
                {
                  "orderNumber": "%s",
                  "customerName": "Apex Manufacturing",
                  "orderDate": "%s",
                  "requiredDeliveryDate": "%s",
                  "priority": "HIGH",
                  "items": [
                    {
                      "productId": %d,
                      "quantity": 10,
                      "unitPrice": 50.00
                    },
                    {
                      "productId": %d,
                      "quantity": 4,
                      "unitPrice": 125.50
                    }
                  ]
                }
                """.formatted(
                orderNumber,
                LocalDate.now(),
                LocalDate.now().plusDays(20),
                productAId,
                productBId
        );

        // (10 * 50.00) + (4 * 125.50) = 500.00 + 502.00 = 1002.00
        long customerOrderId = postJson("/api/customer-orders", createJson)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/customer-orders/")))
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.customerName").value("Apex Manufacturing"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(1002.00))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].product.id").value(productAId))
                .andExpect(jsonPath("$.items[0].lineAmount").value(500.00))
                .andExpect(jsonPath("$.items[1].product.id").value(productBId))
                .andExpect(jsonPath("$.items[1].lineAmount").value(502.00))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .transform(this::extractId);

        // Retrieve by ID
        this.mockMvc.perform(get("/api/customer-orders/" + customerOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerOrderId))
                .andExpect(jsonPath("$.totalAmount").value(1002.00));

        // Retrieve by Number
        this.mockMvc.perform(get("/api/customer-orders/by-number/" + orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerOrderId));

        // Lifecycle: PENDING -> CONFIRMED -> IN_PROGRESS -> FULFILLED
        this.mockMvc.perform(patch("/api/customer-orders/" + customerOrderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        this.mockMvc.perform(patch("/api/customer-orders/" + customerOrderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        this.mockMvc.perform(patch("/api/customer-orders/" + customerOrderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"FULFILLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));

        // Invalid transition: FULFILLED -> PENDING (400)
        this.mockMvc.perform(patch("/api/customer-orders/" + customerOrderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"PENDING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("Invalid customer order transition")));
    }

    @Test
    void customerOrderTransactionRollsBackWhenOneItemInvalid() throws Exception {
        String suffix = suffix();
        long productAId = createProduct(suffix + "-V");
        String orderNumber = "CO-ROLLBACK-" + suffix;

        String invalidJson = """
                {
                  "orderNumber": "%s",
                  "customerName": "Test Rollback",
                  "orderDate": "%s",
                  "requiredDeliveryDate": "%s",
                  "items": [
                    {
                      "productId": %d,
                      "quantity": 10,
                      "unitPrice": 50.00
                    },
                    {
                      "productId": 999999,
                      "quantity": 5,
                      "unitPrice": 100.00
                    }
                  ]
                }
                """.formatted(
                orderNumber,
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                productAId
        );

        // Should return 404 because product 999999 does not exist
        postJson("/api/customer-orders", invalidJson)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(startsWith("Product not found with id: 999999")));

        // Verify order number was not saved (404)
        this.mockMvc.perform(get("/api/customer-orders/by-number/" + orderNumber))
                .andExpect(status().isNotFound());
    }

    @Test
    void validationErrors_return400() throws Exception {
        // Missing required fields
        postJson("/api/production-orders", "{}")
                .andExpect(status().isBadRequest());

        postJson("/api/customer-orders", "{}")
                .andExpect(status().isBadRequest());

        // Empty items in customer order
        String emptyItems = """
                {
                  "orderNumber": "CO-EMPTY-%s",
                  "customerName": "Empty Customer",
                  "items": []
                }
                """.formatted(suffix());
        postJson("/api/customer-orders", emptyItems)
                .andExpect(status().isBadRequest());
    }

    private long createProduct(String suffix) throws Exception {
        String json = """
                {
                  "code": "PR-%s",
                  "name": "Product %s",
                  "category": "Finished Good",
                  "unitCost": 120.00,
                  "sellingPrice": 200.00,
                  "productionTimeHours": 8.0,
                  "status": "ACTIVE"
                }
                """.formatted(suffix, suffix);

        return postJson("/api/products", json)
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
