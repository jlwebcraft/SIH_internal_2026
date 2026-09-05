package com.sih.supplychain.dto.performance;

import com.sih.supplychain.dto.common.SupplierSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SupplierPerformanceResponse(
        Long id,
        SupplierSummaryResponse supplier,
        LocalDate evaluationDate,
        Integer windowDays,
        BigDecimal onTimeDeliveryRate,
        BigDecimal averageDelayDays,
        BigDecimal leadTimeVariance,
        BigDecimal fulfillmentRate,
        BigDecimal rejectionRate,
        BigDecimal capacityUtilization,
        Integer disruptionCount,
        BigDecimal overallScore,
        boolean insufficientHistory,
        int totalOrders,
        int completedDeliveries
) {
}
