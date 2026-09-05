package com.sih.supplychain.dto.customerorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerOrderStatusUpdateRequest(
        @NotBlank
        @Size(max = 50)
        String status
) {
}
