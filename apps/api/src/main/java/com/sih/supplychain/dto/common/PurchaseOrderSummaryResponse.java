package com.sih.supplychain.dto.common;

public record PurchaseOrderSummaryResponse(
        Long id,
        String poNumber,
        String status
) {
}
