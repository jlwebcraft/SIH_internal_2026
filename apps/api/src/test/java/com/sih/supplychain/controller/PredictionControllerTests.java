package com.sih.supplychain.controller;

import com.sih.supplychain.dto.prediction.DisruptionPredictionRequest;
import com.sih.supplychain.dto.prediction.DisruptionPredictionResponse;
import com.sih.supplychain.exception.MlServiceException;
import com.sih.supplychain.exception.MlServiceTimeoutException;
import com.sih.supplychain.exception.MlServiceUnavailableException;
import com.sih.supplychain.service.MlPredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PredictionController.class)
@Import(GlobalExceptionHandler.class)
class PredictionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MlPredictionService predictionService;

    private final String validJsonPayload = """
            {
              "histOtdr90d": 95.5,
              "histAvgDelay90d": 1.2,
              "histFulfillmentRate90d": 98.0,
              "histDisruptions90d": 0,
              "supplierLeadTimeContract": 14,
              "materialCriticality": "HIGH",
              "orderVolumeRatio": 1.5,
              "inventoryCoverageDays": 25.0,
              "poLineValue": 4500.0,
              "supplierCountry": "India"
            }
            """;

    @Test
    void predictDisruption_validRequest_returns200Ok() throws Exception {
        DisruptionPredictionResponse mockResponse = new DisruptionPredictionResponse(
                new BigDecimal("0.3542"),
                0,
                false,
                "LOW",
                "disruption-baseline-v1",
                Instant.parse("2026-09-05T12:00:00Z"),
                0.78
        );

        when(this.predictionService.predictDisruption(any(DisruptionPredictionRequest.class)))
                .thenReturn(mockResponse);

        this.mockMvc.perform(post("/api/predictions/disruption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.validJsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disruptionProbability").value(0.3542))
                .andExpect(jsonPath("$.predictedLabel").value(0))
                .andExpect(jsonPath("$.isDisrupted").value(false))
                .andExpect(jsonPath("$.riskTier").value("LOW"))
                .andExpect(jsonPath("$.modelVersion").value("disruption-baseline-v1"))
                .andExpect(jsonPath("$.confidence").value(0.78));
    }

    @Test
    void predictDisruption_missingRequiredField_returns400BadRequest() throws Exception {
        String invalidPayload = """
                {
                  "histAvgDelay90d": 1.2,
                  "supplierCountry": "India"
                }
                """;

        this.mockMvc.perform(post("/api/predictions/disruption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void predictDisruption_outOfBoundsValue_returns400BadRequest() throws Exception {
        String invalidPayload = """
                {
                  "histOtdr90d": 150.0,
                  "histAvgDelay90d": -2.0,
                  "histFulfillmentRate90d": 98.0,
                  "histDisruptions90d": -1,
                  "supplierLeadTimeContract": 14,
                  "materialCriticality": "HIGH",
                  "orderVolumeRatio": 1.5,
                  "inventoryCoverageDays": 25.0,
                  "poLineValue": 4500.0,
                  "supplierCountry": "India"
                }
                """;

        this.mockMvc.perform(post("/api/predictions/disruption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void predictDisruption_invalidCriticality_returns400BadRequest() throws Exception {
        String invalidPayload = """
                {
                  "histOtdr90d": 95.0,
                  "histAvgDelay90d": 1.0,
                  "histFulfillmentRate90d": 98.0,
                  "histDisruptions90d": 0,
                  "supplierLeadTimeContract": 14,
                  "materialCriticality": "SUPER_CRITICAL",
                  "orderVolumeRatio": 1.5,
                  "inventoryCoverageDays": 25.0,
                  "poLineValue": 4500.0,
                  "supplierCountry": "India"
                }
                """;

        this.mockMvc.perform(post("/api/predictions/disruption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void predictDisruption_mlServiceUnavailable_returns503ServiceUnavailable() throws Exception {
        when(this.predictionService.predictDisruption(any(DisruptionPredictionRequest.class)))
                .thenThrow(new MlServiceUnavailableException("ML prediction service is currently unavailable"));

        this.mockMvc.perform(post("/api/predictions/disruption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.validJsonPayload))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("ML prediction service is currently unavailable"));
    }

    @Test
    void predictDisruption_mlServiceTimeout_returns503ServiceUnavailable() throws Exception {
        when(this.predictionService.predictDisruption(any(DisruptionPredictionRequest.class)))
                .thenThrow(new MlServiceTimeoutException("ML prediction service request timed out"));

        this.mockMvc.perform(post("/api/predictions/disruption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.validJsonPayload))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("ML prediction service request timed out"));
    }

    @Test
    void predictDisruption_mlServiceError_returns502BadGateway() throws Exception {
        when(this.predictionService.predictDisruption(any(DisruptionPredictionRequest.class)))
                .thenThrow(new MlServiceException("ML prediction service failed with HTTP 500"));

        this.mockMvc.perform(post("/api/predictions/disruption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.validJsonPayload))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("ML prediction service failed with HTTP 500"));
    }
}
