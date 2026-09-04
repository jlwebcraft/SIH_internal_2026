package com.sih.supplychain.dto.material;

import java.math.BigDecimal;
import java.time.Instant;

public record MaterialResponse(
        Long id,
        String code,
        String name,
        String description,
        String category,
        String unit,
        BigDecimal unitCost,
        String criticality,
        BigDecimal currentStock,
        BigDecimal safetyStock,
        BigDecimal reorderPoint,
        BigDecimal dailyConsumption,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
