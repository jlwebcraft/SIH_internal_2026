package com.sih.supplychain.dto.customerorder;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CustomerOrderItemRequest(
        @NotNull
        Long productId,

        @NotNull
        @DecimalMin(value = "0.001", inclusive = true)
        BigDecimal quantity,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal unitPrice
) {
}
