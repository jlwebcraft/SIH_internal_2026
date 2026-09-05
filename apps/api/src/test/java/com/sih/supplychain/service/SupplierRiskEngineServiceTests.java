package com.sih.supplychain.service;

import com.sih.supplychain.domain.RiskBand;
import com.sih.supplychain.domain.Supplier;
import com.sih.supplychain.service.SupplierPerformanceCalculatorService.RawPerformanceMetrics;
import com.sih.supplychain.service.SupplierRiskEngineService.RiskEvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SupplierRiskEngineServiceTests {

    @Mock
    private SupplierPerformanceCalculatorService performanceCalculator;

    private SupplierRiskEngineService riskEngineService;

    private Supplier supplier;

    @BeforeEach
    void setUp() {
        this.riskEngineService = new SupplierRiskEngineService(this.performanceCalculator);
        this.supplier = new Supplier("Alpha Supply", "SUP-ALP");
        this.supplier.setReliabilityScore(new BigDecimal("90.00"));
    }

    @Test
    void evaluateRisk_perfectSupplier_yieldsLowRisk() {
        RawPerformanceMetrics metrics = new RawPerformanceMetrics(
                this.supplier,
                LocalDate.of(2026, 9, 1),
                90,
                LocalDate.of(2026, 6, 3),
                LocalDate.of(2026, 9, 1),
                new BigDecimal("100.00"), // OTDR 100% -> Delivery Risk 0
                new BigDecimal("0.00"),   // Delay 0 -> 0
                new BigDecimal("0.00"),   // LTV 0 -> LeadTime Risk 0
                new BigDecimal("100.00"), // Fulfillment 100% -> Fulfillment Risk 0
                null,
                new BigDecimal("50.00"),
                0,                        // Disruption 0 -> Disruption Risk 0
                false,
                10,
                10
        );
        this.supplier.setReliabilityScore(new BigDecimal("100.00")); // Profile Risk 0

        RiskEvaluationResult result = this.riskEngineService.evaluateRisk(this.supplier, metrics);

        assertThat(result.overallScore()).isEqualByComparingTo("0.00");
        assertThat(result.riskLevel()).isEqualTo(RiskBand.LOW);
        assertThat(result.dimensionScores().deliveryRisk()).isEqualByComparingTo("0.00");
        assertThat(result.dimensionScores().disruptionRisk()).isEqualByComparingTo("0.00");
        assertThat(result.dimensionScores().fulfillmentRisk()).isEqualByComparingTo("0.00");
        assertThat(result.dimensionScores().leadTimeRisk()).isEqualByComparingTo("0.00");
        assertThat(result.dimensionScores().profileRisk()).isEqualByComparingTo("0.00");
    }

    @Test
    void evaluateRisk_poorSupplier_yieldsCriticalRisk() {
        RawPerformanceMetrics metrics = new RawPerformanceMetrics(
                this.supplier,
                LocalDate.of(2026, 9, 1),
                90,
                LocalDate.of(2026, 6, 3),
                LocalDate.of(2026, 9, 1),
                new BigDecimal("20.00"), // OTDR 20% -> Rotdr 80, AvgDelay 8.0 -> Rdelay 80 -> DeliveryRisk 80
                new BigDecimal("8.00"),
                new BigDecimal("6.00"),  // LTV 6.0 -> LeadTimeRisk 75
                new BigDecimal("50.00"), // Fulfillment 50% -> FulfillmentRisk 50
                null,
                new BigDecimal("90.00"),
                4,                       // 4 disruptions -> DisruptionRisk 100
                false,
                15,
                10
        );
        this.supplier.setReliabilityScore(new BigDecimal("20.00")); // ProfileRisk 80

        RiskEvaluationResult result = this.riskEngineService.evaluateRisk(this.supplier, metrics);

        // Expected score:
        // Delivery (35%): 80 * 0.35 = 28.00
        // Disruption (25%): 100 * 0.25 = 25.00
        // Fulfillment (20%): 50 * 0.20 = 10.00
        // LeadTime (10%): 75 * 0.10 = 7.50
        // Profile (10%): 80 * 0.10 = 8.00
        // Total = 28.00 + 25.00 + 10.00 + 7.50 + 8.00 = 78.50
        assertThat(result.overallScore()).isEqualByComparingTo("78.50");
        assertThat(result.riskLevel()).isEqualTo(RiskBand.CRITICAL);
        assertThat(result.topRiskDrivers()).isNotEmpty();
    }

    @Test
    void evaluateRisk_weightRedistribution_whenDimensionsUnavailable() {
        // Only Delivery (OTDR 100, Delay 0 -> Risk 0) and Profile (Reliability 80 -> Risk 20) are available.
        // Base weights: Delivery = 0.35, Profile = 0.10 -> Total = 0.45
        // Effective weights:
        // Delivery = 0.35 / 0.45 = 0.7778
        // Profile = 0.10 / 0.45 = 0.2222
        // Score = (0 * 0.7778) + (20 * 0.2222) = 4.44
        RawPerformanceMetrics metrics = new RawPerformanceMetrics(
                this.supplier,
                LocalDate.of(2026, 9, 1),
                90,
                LocalDate.of(2026, 6, 3),
                LocalDate.of(2026, 9, 1),
                new BigDecimal("100.00"),
                new BigDecimal("0.00"),
                null, // LTV null
                null, // FR null
                null,
                null,
                null, // Disruption null
                false,
                2,
                2
        );
        this.supplier.setReliabilityScore(new BigDecimal("80.00"));

        RiskEvaluationResult result = this.riskEngineService.evaluateRisk(this.supplier, metrics);

        assertThat(result.overallScore()).isEqualByComparingTo("4.44");
        assertThat(result.riskLevel()).isEqualTo(RiskBand.LOW);
    }

    @Test
    void evaluateRisk_insufficientHistory_fallsBackToProfileScore() {
        RawPerformanceMetrics metrics = new RawPerformanceMetrics(
                this.supplier,
                LocalDate.of(2026, 9, 1),
                90,
                LocalDate.of(2026, 6, 3),
                LocalDate.of(2026, 9, 1),
                null, null, null, null, null, null, null,
                true, // Insufficient history
                0, 0
        );
        this.supplier.setReliabilityScore(new BigDecimal("60.00")); // Profile Risk = 100 - 60 = 40.00

        RiskEvaluationResult result = this.riskEngineService.evaluateRisk(this.supplier, metrics);

        assertThat(result.insufficientHistory()).isTrue();
        assertThat(result.overallScore()).isEqualByComparingTo("40.00");
        assertThat(result.riskLevel()).isEqualTo(RiskBand.MEDIUM);
        assertThat(result.topRiskDrivers().get(0)).contains("Insufficient historical order/delivery data");
    }

    @Test
    void riskBandThresholds_exactBoundaryValues() {
        assertThat(RiskBand.fromScore(new BigDecimal("0.00"))).isEqualTo(RiskBand.LOW);
        assertThat(RiskBand.fromScore(new BigDecimal("24.99"))).isEqualTo(RiskBand.LOW);
        assertThat(RiskBand.fromScore(new BigDecimal("25.00"))).isEqualTo(RiskBand.MEDIUM);
        assertThat(RiskBand.fromScore(new BigDecimal("49.99"))).isEqualTo(RiskBand.MEDIUM);
        assertThat(RiskBand.fromScore(new BigDecimal("50.00"))).isEqualTo(RiskBand.HIGH);
        assertThat(RiskBand.fromScore(new BigDecimal("74.99"))).isEqualTo(RiskBand.HIGH);
        assertThat(RiskBand.fromScore(new BigDecimal("75.00"))).isEqualTo(RiskBand.CRITICAL);
        assertThat(RiskBand.fromScore(new BigDecimal("100.00"))).isEqualTo(RiskBand.CRITICAL);
    }
}
