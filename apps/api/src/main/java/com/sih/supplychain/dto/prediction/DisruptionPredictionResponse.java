package com.sih.supplychain.dto.prediction;

import java.math.BigDecimal;
import java.time.Instant;

public record DisruptionPredictionResponse(
        BigDecimal disruptionProbability,
        Integer predictedLabel,
        boolean isDisrupted,
        String riskTier,
        String modelVersion,
        Instant inferenceTimestamp,
        Double confidence
) {
}
