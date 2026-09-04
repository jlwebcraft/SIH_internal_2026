package com.sih.supplychain.dto.inventory;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record InventoryUpdateRequest(
        @PositiveOrZero BigDecimal quantityOnHand,
        @PositiveOrZero BigDecimal quantityReserved,
        @PositiveOrZero BigDecimal quantityIncoming,
        @PositiveOrZero BigDecimal safetyStock,
        @PositiveOrZero BigDecimal reorderPoint
) {
}
