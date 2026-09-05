package com.sih.supplychain.dto.productionorder;

import com.sih.supplychain.dto.common.ProductSummaryResponse;
import com.sih.supplychain.dto.common.UserSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductionOrderResponse(
        Long id,
        String productionNumber,
        ProductSummaryResponse product,
        BigDecimal quantity,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        LocalDate actualStartDate,
        LocalDate actualEndDate,
        String status,
        String priority,
        UserSummaryResponse createdBy
) {
}
