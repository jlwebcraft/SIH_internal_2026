package com.sih.supplychain.dto.suppliermaterial;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SupplierMaterialCreateRequest(
        @PositiveOrZero BigDecimal unitPrice,
        @PositiveOrZero Integer leadTimeDays,
        @PositiveOrZero BigDecimal minimumOrderQuantity,
        @PositiveOrZero BigDecimal maximumCapacity,
        @PositiveOrZero @DecimalMax("100.00") BigDecimal reliabilityScore,
        @Size(max = 50) String status
) {
}
