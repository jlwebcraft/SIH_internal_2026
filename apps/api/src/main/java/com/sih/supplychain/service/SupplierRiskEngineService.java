package com.sih.supplychain.service;

import com.sih.supplychain.domain.RiskBand;
import com.sih.supplychain.domain.Supplier;
import com.sih.supplychain.dto.risk.RiskDimensionScores;
import com.sih.supplychain.dto.risk.RiskDimensionWeights;
import com.sih.supplychain.dto.risk.SupplierRiskResponse;
import com.sih.supplychain.mapper.OperationalMapper;
import com.sih.supplychain.service.SupplierPerformanceCalculatorService.RawPerformanceMetrics;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupplierRiskEngineService {

    public static final BigDecimal DEFAULT_DELIVERY_WEIGHT = new BigDecimal("0.35");
    public static final BigDecimal DEFAULT_DISRUPTION_WEIGHT = new BigDecimal("0.25");
    public static final BigDecimal DEFAULT_FULFILLMENT_WEIGHT = new BigDecimal("0.20");
    public static final BigDecimal DEFAULT_LEAD_TIME_WEIGHT = new BigDecimal("0.10");
    public static final BigDecimal DEFAULT_PROFILE_WEIGHT = new BigDecimal("0.10");

    private final SupplierPerformanceCalculatorService performanceCalculator;

    public SupplierRiskEngineService(SupplierPerformanceCalculatorService performanceCalculator) {
        this.performanceCalculator = performanceCalculator;
    }

    public RiskEvaluationResult evaluateRisk(Supplier supplier, RawPerformanceMetrics metrics) {
        RawPerformanceMetrics performance = metrics != null ? metrics : this.performanceCalculator.calculatePerformance(supplier, null);

        BigDecimal deliveryRisk = calculateDeliveryRisk(performance.onTimeDeliveryRate(), performance.averageDelayDays());
        BigDecimal disruptionRisk = calculateDisruptionRisk(performance.disruptionCount(), performance.insufficientHistory());
        BigDecimal fulfillmentRisk = calculateFulfillmentRisk(performance.fulfillmentRate());
        BigDecimal leadTimeRisk = calculateLeadTimeRisk(performance.leadTimeVariance());
        BigDecimal profileRisk = calculateProfileRisk(supplier.getReliabilityScore());

        Map<String, BigDecimal> activeDimensions = new HashMap<>();
        Map<String, BigDecimal> baseWeights = new HashMap<>();

        if (deliveryRisk != null) {
            activeDimensions.put("delivery", deliveryRisk);
            baseWeights.put("delivery", DEFAULT_DELIVERY_WEIGHT);
        }
        if (disruptionRisk != null) {
            activeDimensions.put("disruption", disruptionRisk);
            baseWeights.put("disruption", DEFAULT_DISRUPTION_WEIGHT);
        }
        if (fulfillmentRisk != null) {
            activeDimensions.put("fulfillment", fulfillmentRisk);
            baseWeights.put("fulfillment", DEFAULT_FULFILLMENT_WEIGHT);
        }
        if (leadTimeRisk != null) {
            activeDimensions.put("leadTime", leadTimeRisk);
            baseWeights.put("leadTime", DEFAULT_LEAD_TIME_WEIGHT);
        }
        if (profileRisk != null) {
            activeDimensions.put("profile", profileRisk);
            baseWeights.put("profile", DEFAULT_PROFILE_WEIGHT);
        }

        // Calculate normalized effective weights
        BigDecimal totalActiveWeight = baseWeights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> effectiveWeights = new HashMap<>();
        BigDecimal overallScore = BigDecimal.ZERO;

        if (totalActiveWeight.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<String, BigDecimal> entry : baseWeights.entrySet()) {
                String dim = entry.getKey();
                BigDecimal normalizedWeight = entry.getValue().divide(totalActiveWeight, 4, RoundingMode.HALF_UP);
                effectiveWeights.put(dim, normalizedWeight);
                overallScore = overallScore.add(activeDimensions.get(dim).multiply(normalizedWeight));
            }
        } else {
            overallScore = new BigDecimal("50.00"); // Neutral baseline
        }

        overallScore = overallScore.setScale(2, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(new BigDecimal("100.00"));

        RiskBand riskBand = RiskBand.fromScore(overallScore);
        List<String> topRiskDrivers = generateTopRiskDrivers(performance, supplier, deliveryRisk, disruptionRisk, fulfillmentRisk, leadTimeRisk, profileRisk);
        List<String> recommendations = generateRecommendations(riskBand, performance);

        RiskDimensionScores dimScores = new RiskDimensionScores(
                deliveryRisk,
                disruptionRisk,
                fulfillmentRisk,
                leadTimeRisk,
                profileRisk
        );

        RiskDimensionWeights dimWeights = new RiskDimensionWeights(
                effectiveWeights.getOrDefault("delivery", BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                effectiveWeights.getOrDefault("disruption", BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                effectiveWeights.getOrDefault("fulfillment", BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                effectiveWeights.getOrDefault("leadTime", BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                effectiveWeights.getOrDefault("profile", BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
        );

        return new RiskEvaluationResult(
                supplier,
                performance.evaluationDate(),
                overallScore,
                riskBand,
                performance.insufficientHistory(),
                dimScores,
                dimWeights,
                performance,
                topRiskDrivers,
                recommendations,
                Instant.now()
        );
    }

    private BigDecimal calculateDeliveryRisk(BigDecimal otdr, BigDecimal avgDelay) {
        if (otdr == null && avgDelay == null) {
            return null;
        }
        if (otdr != null && avgDelay != null) {
            BigDecimal rOtdr = BigDecimal.valueOf(100.00).subtract(otdr);
            BigDecimal rDelay = avgDelay.multiply(BigDecimal.valueOf(10.0)).min(BigDecimal.valueOf(100.00));
            return rOtdr.multiply(BigDecimal.valueOf(0.50))
                    .add(rDelay.multiply(BigDecimal.valueOf(0.50)))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        if (otdr != null) {
            return BigDecimal.valueOf(100.00).subtract(otdr).setScale(2, RoundingMode.HALF_UP);
        }
        return avgDelay.multiply(BigDecimal.valueOf(10.0)).min(BigDecimal.valueOf(100.00)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDisruptionRisk(Integer disruptionCount, boolean insufficientHistory) {
        if (insufficientHistory || disruptionCount == null) {
            return null;
        }
        return BigDecimal.valueOf(disruptionCount)
                .multiply(BigDecimal.valueOf(25.0))
                .min(BigDecimal.valueOf(100.00))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFulfillmentRisk(BigDecimal fulfillmentRate) {
        if (fulfillmentRate == null) {
            return null;
        }
        return BigDecimal.valueOf(100.00).subtract(fulfillmentRate).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLeadTimeRisk(BigDecimal leadTimeVariance) {
        if (leadTimeVariance == null) {
            return null;
        }
        return leadTimeVariance.multiply(BigDecimal.valueOf(12.5))
                .min(BigDecimal.valueOf(100.00))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateProfileRisk(BigDecimal reliabilityScore) {
        if (reliabilityScore == null) {
            return new BigDecimal("50.00");
        }
        return BigDecimal.valueOf(100.00).subtract(reliabilityScore)
                .max(BigDecimal.ZERO)
                .min(BigDecimal.valueOf(100.00))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> generateTopRiskDrivers(
            RawPerformanceMetrics metrics,
            Supplier supplier,
            BigDecimal deliveryRisk,
            BigDecimal disruptionRisk,
            BigDecimal fulfillmentRisk,
            BigDecimal leadTimeRisk,
            BigDecimal profileRisk
    ) {
        List<String> drivers = new ArrayList<>();

        if (metrics.insufficientHistory()) {
            drivers.add("Insufficient historical order/delivery data in 90-day window; risk is estimated from baseline profile score");
            return drivers;
        }

        if (metrics.onTimeDeliveryRate() != null && metrics.onTimeDeliveryRate().compareTo(new BigDecimal("80.00")) < 0) {
            drivers.add("On-time delivery rate is " + metrics.onTimeDeliveryRate() + "% across " + metrics.completedDeliveries() + " completed shipments (below 80.00% benchmark)");
        }
        if (metrics.averageDelayDays() != null && metrics.averageDelayDays().compareTo(new BigDecimal("2.00")) > 0) {
            drivers.add("Average delivery delay is " + metrics.averageDelayDays() + " days across completed shipments");
        }
        if (metrics.disruptionCount() != null && metrics.disruptionCount() > 0) {
            drivers.add(metrics.disruptionCount() + " critical disruption event(s) recorded in the last 90 days");
        }
        if (metrics.fulfillmentRate() != null && metrics.fulfillmentRate().compareTo(new BigDecimal("95.00")) < 0) {
            drivers.add("Fulfillment rate is " + metrics.fulfillmentRate() + "% (below 95.00% target)");
        }
        if (metrics.leadTimeVariance() != null && metrics.leadTimeVariance().compareTo(new BigDecimal("3.00")) > 0) {
            drivers.add("Lead-time variance of " + metrics.leadTimeVariance() + " days against contracted schedule");
        }
        if (profileRisk != null && profileRisk.compareTo(new BigDecimal("40.00")) > 0) {
            drivers.add("Supplier profile baseline reliability score is low (" + supplier.getReliabilityScore() + " / 100)");
        }

        if (drivers.isEmpty()) {
            drivers.add("Supplier demonstrates strong operational performance across all delivery, fulfillment, and lead-time metrics");
        }

        return drivers;
    }

    private List<String> generateRecommendations(RiskBand riskBand, RawPerformanceMetrics metrics) {
        List<String> recommendations = new ArrayList<>();
        switch (riskBand) {
            case CRITICAL -> {
                recommendations.add("Initiate dual-sourcing / alternative supplier recommendation immediately for critical BOM materials");
                recommendations.add("Implement mandatory buffer stock safety inventory increase");
                recommendations.add("Issue formal performance audit and mitigation review request to supplier");
            }
            case HIGH -> {
                recommendations.add("Increase inventory safety stock coverage for materials supplied by this vendor");
                recommendations.add("Monitor upcoming purchase order delivery milestones with active tracking");
            }
            case MEDIUM -> {
                recommendations.add("Standard operational monitoring; review quarterly lead-time variance trends");
            }
            case LOW -> {
                recommendations.add("Preferred strategic supplier; eligible for primary volume allocation in production planning");
            }
        }
        return recommendations;
    }

    public record RiskEvaluationResult(
            Supplier supplier,
            java.time.LocalDate evaluationDate,
            BigDecimal overallScore,
            RiskBand riskLevel,
            boolean insufficientHistory,
            RiskDimensionScores dimensionScores,
            RiskDimensionWeights effectiveWeights,
            RawPerformanceMetrics underlyingMetrics,
            List<String> topRiskDrivers,
            List<String> recommendations,
            Instant calculatedAt
    ) {
    }
}
