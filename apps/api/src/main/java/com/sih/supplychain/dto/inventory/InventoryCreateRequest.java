package com.sih.supplychain.dto.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InventoryCreateRequest(
        @NotNull Long materialId,
        @NotBlank @Size(max = 200) String warehouseLocation,
        @PositiveOrZero BigDecimal quantityOnHand,
        @PositiveOrZero BigDecimal quantityReserved,
        @PositiveOrZero BigDecimal quantityIncoming,
        @PositiveOrZero BigDecimal safetyStock,
        @PositiveOrZero BigDecimal reorderPoint
) {
}
