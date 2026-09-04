package com.sih.supplychain.dto.suppliermaterial;

import com.sih.supplychain.dto.common.MaterialSummaryResponse;
import com.sih.supplychain.dto.common.SupplierSummaryResponse;

import java.math.BigDecimal;
import java.time.Instant;

public record SupplierMaterialResponse(
        Long id,
        SupplierSummaryResponse supplier,
        MaterialSummaryResponse material,
        BigDecimal unitPrice,
        Integer leadTimeDays,
        BigDecimal minimumOrderQuantity,
        BigDecimal maximumCapacity,
        BigDecimal reliabilityScore,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
