package com.sih.supplychain.dto.customerorder;

import com.sih.supplychain.dto.common.ProductSummaryResponse;

import java.math.BigDecimal;

public record CustomerOrderItemResponse(
        Long id,
        ProductSummaryResponse product,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineAmount
) {
}
