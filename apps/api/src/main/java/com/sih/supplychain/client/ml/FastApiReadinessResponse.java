package com.sih.supplychain.client.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiReadinessResponse(
        @JsonProperty("status") String status,
        @JsonProperty("service") String service,
        @JsonProperty("version") String version,
        @JsonProperty("model_available") Boolean modelAvailable,
        @JsonProperty("details") String details
) {
}
