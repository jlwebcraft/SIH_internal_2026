package com.sih.supplychain.dto.productionorder;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductionOrderCreateRequest(
        @NotBlank
        @Size(max = 80)
        String productionNumber,

        @NotNull
        Long productId,

        @NotNull
        @DecimalMin(value = "0.001", inclusive = true)
        BigDecimal quantity,

        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        LocalDate actualStartDate,
        LocalDate actualEndDate,

        @Size(max = 50)
        String status,

        @Size(max = 50)
        String priority,

        Long createdBy
) {
}
