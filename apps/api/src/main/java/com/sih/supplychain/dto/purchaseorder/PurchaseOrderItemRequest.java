package com.sih.supplychain.dto.purchaseorder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseOrderItemRequest(
        @NotNull
        @Positive
        Long materialId,

        @NotNull
        @Positive
        BigDecimal quantity,

        @NotNull
        @PositiveOrZero
        BigDecimal unitPrice,

        LocalDate expectedDate
) {
}
