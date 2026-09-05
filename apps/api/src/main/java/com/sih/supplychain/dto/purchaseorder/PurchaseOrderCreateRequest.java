package com.sih.supplychain.dto.purchaseorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderCreateRequest(
        @NotBlank
        @Size(max = 80)
        String poNumber,

        @NotNull
        @Positive
        Long supplierId,

        LocalDate orderDate,

        LocalDate expectedDeliveryDate,

        @NotEmpty
        List<@Valid PurchaseOrderItemRequest> items
) {
}
