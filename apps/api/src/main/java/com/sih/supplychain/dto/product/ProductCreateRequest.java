package com.sih.supplychain.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String description,
        @Size(max = 120) String category,
        @PositiveOrZero BigDecimal unitCost,
        @PositiveOrZero BigDecimal sellingPrice,
        @PositiveOrZero BigDecimal productionTimeHours,
        @Size(max = 50) String status
) {
}
