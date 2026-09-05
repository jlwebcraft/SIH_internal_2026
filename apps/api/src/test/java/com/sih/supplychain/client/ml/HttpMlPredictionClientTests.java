package com.sih.supplychain.client.ml;

import com.sih.supplychain.exception.MlServiceException;
import com.sih.supplychain.exception.MlServiceTimeoutException;
import com.sih.supplychain.exception.MlServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpMlPredictionClientTests {

    private MockRestServiceServer mockServer;
    private HttpMlPredictionClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        this.mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        this.client = new HttpMlPredictionClient(restClient);
    }

    @Test
    void predictDisruption_success() {
        String responseJson = """
                {
                  "disruption_probability": 0.3542,
                  "predicted_label": 0,
                  "risk_tier": "LOW",
                  "model_version": "disruption-baseline-v1",
                  "inference_timestamp": "2026-09-05T12:00:00Z",
                  "confidence": 0.78
                }
                """;

        this.mockServer.expect(requestTo("http://localhost:8000/api/predict/disruption"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hist_otdr_90d").value(95.5))
                .andExpect(jsonPath("$.hist_avg_delay_90d").value(1.2))
                .andExpect(jsonPath("$.hist_fulfillment_rate_90d").value(98.0))
                .andExpect(jsonPath("$.hist_disruptions_90d").value(0))
                .andExpect(jsonPath("$.supplier_lead_time_contract").value(14))
                .andExpect(jsonPath("$.material_criticality").value("HIGH"))
                .andExpect(jsonPath("$.order_volume_ratio").value(1.5))
                .andExpect(jsonPath("$.inventory_coverage_days").value(25.0))
                .andExpect(jsonPath("$.po_line_value").value(4500.0))
                .andExpect(jsonPath("$.supplier_country").value("India"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        FastApiPredictionRequest request = new FastApiPredictionRequest(
                95.5, 1.2, 98.0, 0, 14, "HIGH", 1.5, 25.0, 4500.0, "India"
        );

        FastApiPredictionResponse response = this.client.predictDisruption(request);

        this.mockServer.verify();
        assertThat(response).isNotNull();
        assertThat(response.disruptionProbability()).isEqualTo(0.3542);
        assertThat(response.predictedLabel()).isEqualTo(0);
        assertThat(response.riskTier()).isEqualTo("LOW");
        assertThat(response.modelVersion()).isEqualTo("disruption-baseline-v1");
        assertThat(response.inferenceTimestamp()).isEqualTo(Instant.parse("2026-09-05T12:00:00Z"));
        assertThat(response.confidence()).isEqualTo(0.78);
    }

    @Test
    void predictDisruption_serviceUnavailable_throwsMlServiceUnavailableException() {
        this.mockServer.expect(requestTo("http://localhost:8000/api/predict/disruption"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServiceUnavailable());

        FastApiPredictionRequest request = new FastApiPredictionRequest(
                90.0, 2.0, 95.0, 1, 10, "MEDIUM", 1.0, 15.0, 3000.0, "India"
        );

        assertThatThrownBy(() -> this.client.predictDisruption(request))
                .isInstanceOf(MlServiceUnavailableException.class)
                .hasMessageContaining("unavailable");

        this.mockServer.verify();
    }

    @Test
    void predictDisruption_gatewayTimeout_throwsMlServiceTimeoutException() {
        this.mockServer.expect(requestTo("http://localhost:8000/api/predict/disruption"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));

        FastApiPredictionRequest request = new FastApiPredictionRequest(
                90.0, 2.0, 95.0, 1, 10, "MEDIUM", 1.0, 15.0, 3000.0, "India"
        );

        assertThatThrownBy(() -> this.client.predictDisruption(request))
                .isInstanceOf(MlServiceTimeoutException.class)
                .hasMessageContaining("timed out");

        this.mockServer.verify();
    }

    @Test
    void predictDisruption_badRequest4xx_throwsMlServiceException() {
        this.mockServer.expect(requestTo("http://localhost:8000/api/predict/disruption"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body("{\"detail\":\"validation error\"}"));

        FastApiPredictionRequest request = new FastApiPredictionRequest(
                90.0, 2.0, 95.0, 1, 10, "MEDIUM", 1.0, 15.0, 3000.0, "India"
        );

        assertThatThrownBy(() -> this.client.predictDisruption(request))
                .isInstanceOf(MlServiceException.class)
                .hasMessageContaining("rejected the feature payload");

        this.mockServer.verify();
    }

    @Test
    void predictDisruption_serverError5xx_throwsMlServiceException() {
        this.mockServer.expect(requestTo("http://localhost:8000/api/predict/disruption"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        FastApiPredictionRequest request = new FastApiPredictionRequest(
                90.0, 2.0, 95.0, 1, 10, "MEDIUM", 1.0, 15.0, 3000.0, "India"
        );

        assertThatThrownBy(() -> this.client.predictDisruption(request))
                .isInstanceOf(MlServiceException.class)
                .hasMessageContaining("failed with HTTP 500");

        this.mockServer.verify();
    }

    @Test
    void predictDisruption_malformedJson_throwsMlServiceException() {
        this.mockServer.expect(requestTo("http://localhost:8000/api/predict/disruption"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("Not Valid JSON", MediaType.APPLICATION_JSON));

        FastApiPredictionRequest request = new FastApiPredictionRequest(
                90.0, 2.0, 95.0, 1, 10, "MEDIUM", 1.0, 15.0, 3000.0, "India"
        );

        assertThatThrownBy(() -> this.client.predictDisruption(request))
                .isInstanceOf(MlServiceException.class);

        this.mockServer.verify();
    }

    @Test
    void checkReadiness_success() {
        String responseJson = """
                {
                  "status": "READY",
                  "service": "ml-service",
                  "version": "1.0.0",
                  "model_available": true,
                  "details": "Model disruption-baseline-v1 loaded"
                }
                """;

        this.mockServer.expect(requestTo("http://localhost:8000/api/ready"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        FastApiReadinessResponse response = this.client.checkReadiness();

        this.mockServer.verify();
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("READY");
        assertThat(response.modelAvailable()).isTrue();
        assertThat(response.details()).contains("disruption-baseline-v1");
    }
}
