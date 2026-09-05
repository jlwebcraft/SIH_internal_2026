package com.sih.supplychain.dto.risk;

import com.sih.supplychain.domain.RiskBand;
import com.sih.supplychain.dto.common.SupplierSummaryResponse;
import com.sih.supplychain.dto.performance.SupplierPerformanceResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SupplierRiskResponse(
        SupplierSummaryResponse supplier,
        LocalDate evaluationDate,
        BigDecimal overallScore,
        RiskBand riskLevel,
        boolean insufficientHistory,
        RiskDimensionScores dimensionScores,
        RiskDimensionWeights effectiveWeights,
        SupplierPerformanceResponse underlyingMetrics,
        List<String> topRiskDrivers,
        List<String> recommendations,
        Instant calculatedAt
) {
}
