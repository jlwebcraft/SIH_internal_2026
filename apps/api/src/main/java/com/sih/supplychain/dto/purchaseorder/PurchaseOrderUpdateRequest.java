package com.sih.supplychain.dto.purchaseorder;

import java.time.LocalDate;

public record PurchaseOrderUpdateRequest(
        LocalDate expectedDeliveryDate
) {
}
