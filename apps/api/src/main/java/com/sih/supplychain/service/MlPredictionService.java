package com.sih.supplychain.service;

import com.sih.supplychain.client.ml.FastApiPredictionRequest;
import com.sih.supplychain.client.ml.FastApiPredictionResponse;
import com.sih.supplychain.client.ml.MlPredictionClient;
import com.sih.supplychain.dto.prediction.DisruptionPredictionRequest;
import com.sih.supplychain.dto.prediction.DisruptionPredictionResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;

@Service
public class MlPredictionService {

    private final MlPredictionClient mlPredictionClient;

    public MlPredictionService(MlPredictionClient mlPredictionClient) {
        this.mlPredictionClient = mlPredictionClient;
    }

    public DisruptionPredictionResponse predictDisruption(DisruptionPredictionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("DisruptionPredictionRequest must not be null");
        }

        FastApiPredictionRequest clientRequest = new FastApiPredictionRequest(
                request.histOtdr90d().doubleValue(),
                request.histAvgDelay90d().doubleValue(),
                request.histFulfillmentRate90d().doubleValue(),
                request.histDisruptions90d(),
                request.supplierLeadTimeContract(),
                request.materialCriticality().trim().toUpperCase(Locale.ROOT),
                request.orderVolumeRatio().doubleValue(),
                request.inventoryCoverageDays().doubleValue(),
                request.poLineValue().doubleValue(),
                request.supplierCountry().trim()
        );

        FastApiPredictionResponse clientResponse = this.mlPredictionClient.predictDisruption(clientRequest);

        BigDecimal probability = clientResponse.disruptionProbability() != null
                ? BigDecimal.valueOf(clientResponse.disruptionProbability()).setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int label = clientResponse.predictedLabel() != null ? clientResponse.predictedLabel() : 0;
        boolean isDisrupted = label == 1;

        Instant timestamp = clientResponse.inferenceTimestamp() != null
                ? clientResponse.inferenceTimestamp()
                : Instant.now();

        return new DisruptionPredictionResponse(
                probability,
                label,
                isDisrupted,
                clientResponse.riskTier(),
                clientResponse.modelVersion(),
                timestamp,
                clientResponse.confidence()
        );
    }
}
