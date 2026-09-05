package com.sih.supplychain.client.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiPredictionResponse(
        @JsonProperty("disruption_probability") Double disruptionProbability,
        @JsonProperty("predicted_label") Integer predictedLabel,
        @JsonProperty("risk_tier") String riskTier,
        @JsonProperty("model_version") String modelVersion,
        @JsonProperty("inference_timestamp") Instant inferenceTimestamp,
        @JsonProperty("confidence") Double confidence
) {
}
