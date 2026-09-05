package com.sih.supplychain.dto.customerorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CustomerOrderUpdateRequest(
        @NotBlank
        @Size(max = 200)
        String customerName,

        LocalDate orderDate,
        LocalDate requiredDeliveryDate,

        @Size(max = 50)
        String priority
) {
}
