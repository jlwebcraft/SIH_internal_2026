package com.sih.supplychain.dto.customerorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CustomerOrderCreateRequest(
        @NotBlank
        @Size(max = 80)
        String orderNumber,

        @NotBlank
        @Size(max = 200)
        String customerName,

        LocalDate orderDate,
        LocalDate requiredDeliveryDate,

        @Size(max = 50)
        String status,

        @Size(max = 50)
        String priority,

        @NotEmpty
        List<@Valid CustomerOrderItemRequest> items
) {
}
