package com.sih.supplychain.dto.delivery;

import com.sih.supplychain.dto.common.PurchaseOrderSummaryResponse;

import java.time.LocalDate;

public record DeliveryResponse(
        Long id,
        PurchaseOrderSummaryResponse purchaseOrder,
        String trackingNumber,
        LocalDate dispatchDate,
        LocalDate expectedArrivalDate,
        LocalDate actualArrivalDate,
        String status,
        Integer delayDays,
        String notes
) {
}
