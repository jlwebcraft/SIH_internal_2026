package com.sih.supplychain.dto.risk;

import java.math.BigDecimal;

public record RiskDimensionScores(
        BigDecimal deliveryRisk,
        BigDecimal disruptionRisk,
        BigDecimal fulfillmentRisk,
        BigDecimal leadTimeRisk,
        BigDecimal profileRisk
) {
}
