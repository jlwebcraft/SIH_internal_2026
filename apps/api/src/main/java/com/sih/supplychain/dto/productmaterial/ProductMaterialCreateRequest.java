package com.sih.supplychain.dto.productmaterial;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductMaterialCreateRequest(
        @NotNull @Positive BigDecimal quantityRequired,
        @Size(max = 50) String unit,
        @PositiveOrZero @DecimalMax("100.00") BigDecimal wastagePercentage
) {
}
