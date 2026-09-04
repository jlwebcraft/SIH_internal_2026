package com.sih.supplychain.dto.material;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MaterialCreateRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String description,
        @Size(max = 120) String category,
        @Size(max = 50) String unit,
        @PositiveOrZero BigDecimal unitCost,
        @Size(max = 50) String criticality,
        @PositiveOrZero BigDecimal currentStock,
        @PositiveOrZero BigDecimal safetyStock,
        @PositiveOrZero BigDecimal reorderPoint,
        @PositiveOrZero BigDecimal dailyConsumption,
        @Size(max = 50) String status
) {
}
