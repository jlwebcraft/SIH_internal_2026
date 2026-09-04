package com.sih.supplychain.dto.productmaterial;

import com.sih.supplychain.dto.common.MaterialSummaryResponse;
import com.sih.supplychain.dto.common.ProductSummaryResponse;

import java.math.BigDecimal;

public record ProductMaterialResponse(
        Long id,
        ProductSummaryResponse product,
        MaterialSummaryResponse material,
        BigDecimal quantityRequired,
        String unit,
        BigDecimal wastagePercentage
) {
}
