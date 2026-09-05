package com.sih.supplychain.dto.risk;

import java.math.BigDecimal;

public record RiskDimensionWeights(
        BigDecimal deliveryWeight,
        BigDecimal disruptionWeight,
        BigDecimal fulfillmentWeight,
        BigDecimal leadTimeWeight,
        BigDecimal profileWeight
) {
}
