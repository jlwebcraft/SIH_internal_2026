package com.sih.supplychain.dto.inventory;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InventoryAdjustmentRequest(
        @NotNull BigDecimal quantityChange
) {
}
