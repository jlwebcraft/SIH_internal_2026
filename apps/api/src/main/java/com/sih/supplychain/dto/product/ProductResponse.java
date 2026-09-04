package com.sih.supplychain.dto.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String code,
        String name,
        String description,
        String category,
        BigDecimal unitCost,
        BigDecimal sellingPrice,
        BigDecimal productionTimeHours,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
