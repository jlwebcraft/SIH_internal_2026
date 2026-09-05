package com.sih.supplychain.dto.prediction;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DisruptionPredictionRequest(
        @NotNull(message = "histOtdr90d is required")
        @DecimalMin(value = "0.0", message = "histOtdr90d must be greater than or equal to 0.0")
        @DecimalMax(value = "100.0", message = "histOtdr90d must be less than or equal to 100.0")
        BigDecimal histOtdr90d,

        @NotNull(message = "histAvgDelay90d is required")
        @DecimalMin(value = "0.0", message = "histAvgDelay90d must be greater than or equal to 0.0")
        BigDecimal histAvgDelay90d,

        @NotNull(message = "histFulfillmentRate90d is required")
        @DecimalMin(value = "0.0", message = "histFulfillmentRate90d must be greater than or equal to 0.0")
        @DecimalMax(value = "100.0", message = "histFulfillmentRate90d must be less than or equal to 100.0")
        BigDecimal histFulfillmentRate90d,

        @NotNull(message = "histDisruptions90d is required")
        @Min(value = 0, message = "histDisruptions90d must be non-negative")
        Integer histDisruptions90d,

        @NotNull(message = "supplierLeadTimeContract is required")
        @Min(value = 0, message = "supplierLeadTimeContract must be non-negative")
        Integer supplierLeadTimeContract,

        @NotBlank(message = "materialCriticality is required")
        @Pattern(regexp = "(?i)HIGH|MEDIUM|LOW", message = "materialCriticality must be HIGH, MEDIUM, or LOW")
        String materialCriticality,

        @NotNull(message = "orderVolumeRatio is required")
        @DecimalMin(value = "0.0", message = "orderVolumeRatio must be greater than or equal to 0.0")
        BigDecimal orderVolumeRatio,

        @NotNull(message = "inventoryCoverageDays is required")
        @DecimalMin(value = "0.0", message = "inventoryCoverageDays must be greater than or equal to 0.0")
        BigDecimal inventoryCoverageDays,

        @NotNull(message = "poLineValue is required")
        @DecimalMin(value = "0.0", message = "poLineValue must be greater than or equal to 0.0")
        BigDecimal poLineValue,

        @NotBlank(message = "supplierCountry is required")
        @Size(max = 100, message = "supplierCountry must not exceed 100 characters")
        String supplierCountry
) {
}
