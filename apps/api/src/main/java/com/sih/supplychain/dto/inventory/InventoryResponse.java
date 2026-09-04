package com.sih.supplychain.dto.inventory;

import com.sih.supplychain.dto.common.MaterialSummaryResponse;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryResponse(
        Long id,
        MaterialSummaryResponse material,
        String warehouseLocation,
        BigDecimal quantityOnHand,
        BigDecimal quantityReserved,
        BigDecimal quantityIncoming,
        BigDecimal safetyStock,
        BigDecimal reorderPoint,
        Instant lastUpdated
) {
}
