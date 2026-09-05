package com.sih.supplychain.dto.purchaseorder;

import com.sih.supplychain.dto.common.SupplierSummaryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        String poNumber,
        SupplierSummaryResponse supplier,
        String status,
        LocalDate orderDate,
        LocalDate expectedDeliveryDate,
        LocalDate actualDeliveryDate,
        BigDecimal totalAmount,
        List<PurchaseOrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
