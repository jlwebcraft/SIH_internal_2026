package com.sih.supplychain.service;

import com.sih.supplychain.client.ml.FastApiPredictionRequest;
import com.sih.supplychain.client.ml.FastApiPredictionResponse;
import com.sih.supplychain.client.ml.MlPredictionClient;
import com.sih.supplychain.dto.prediction.DisruptionPredictionRequest;
import com.sih.supplychain.dto.prediction.DisruptionPredictionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MlPredictionServiceTests {

    @Mock
    private MlPredictionClient mlPredictionClient;

    private MlPredictionService service;

    @BeforeEach
    void setUp() {
        this.service = new MlPredictionService(this.mlPredictionClient);
    }

    @Test
    void predictDisruption_mapsRequestAndResponseCorrectly() {
        Instant now = Instant.now();
        FastApiPredictionResponse clientResponse = new FastApiPredictionResponse(
                0.46238,
                1,
                "HIGH",
                "disruption-baseline-v1",
                now,
                0.85
        );

        when(this.mlPredictionClient.predictDisruption(any(FastApiPredictionRequest.class)))
                .thenReturn(clientResponse);

        DisruptionPredictionRequest request = new DisruptionPredictionRequest(
                new BigDecimal("94.5"),
                new BigDecimal("1.8"),
                new BigDecimal("97.2"),
                2,
                21,
                "high",
                new BigDecimal("1.25"),
                new BigDecimal("18.5"),
                new BigDecimal("12500.00"),
                "India"
        );

        DisruptionPredictionResponse response = this.service.predictDisruption(request);

        ArgumentCaptor<FastApiPredictionRequest> captor = ArgumentCaptor.forClass(FastApiPredictionRequest.class);
        verify(this.mlPredictionClient).predictDisruption(captor.capture());

        FastApiPredictionRequest sent = captor.getValue();
        assertThat(sent.histOtdr90d()).isEqualTo(94.5);
        assertThat(sent.histAvgDelay90d()).isEqualTo(1.8);
        assertThat(sent.histFulfillmentRate90d()).isEqualTo(97.2);
        assertThat(sent.histDisruptions90d()).isEqualTo(2);
        assertThat(sent.supplierLeadTimeContract()).isEqualTo(21);
        assertThat(sent.materialCriticality()).isEqualTo("HIGH");
        assertThat(sent.orderVolumeRatio()).isEqualTo(1.25);
        assertThat(sent.inventoryCoverageDays()).isEqualTo(18.5);
        assertThat(sent.poLineValue()).isEqualTo(12500.0);
        assertThat(sent.supplierCountry()).isEqualTo("India");

        assertThat(response).isNotNull();
        assertThat(response.disruptionProbability()).isEqualByComparingTo(new BigDecimal("0.4624"));
        assertThat(response.predictedLabel()).isEqualTo(1);
        assertThat(response.isDisrupted()).isTrue();
        assertThat(response.riskTier()).isEqualTo("HIGH");
        assertThat(response.modelVersion()).isEqualTo("disruption-baseline-v1");
        assertThat(response.inferenceTimestamp()).isEqualTo(now);
        assertThat(response.confidence()).isEqualTo(0.85);
    }

    @Test
    void predictDisruption_nullRequest_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> this.service.predictDisruption(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
