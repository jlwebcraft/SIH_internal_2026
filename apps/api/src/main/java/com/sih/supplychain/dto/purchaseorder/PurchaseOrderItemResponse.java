package com.sih.supplychain.dto.purchaseorder;

import com.sih.supplychain.dto.common.MaterialSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseOrderItemResponse(
        Long id,
        MaterialSummaryResponse material,
        BigDecimal quantity,
        BigDecimal unitPrice,
        LocalDate expectedDate,
        BigDecimal receivedQuantity,
        String status,
        BigDecimal lineAmount
) {
}
