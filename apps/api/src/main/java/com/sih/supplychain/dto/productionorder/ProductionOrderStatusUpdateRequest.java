package com.sih.supplychain.dto.productionorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductionOrderStatusUpdateRequest(
        @NotBlank
        @Size(max = 50)
        String status
) {
}
